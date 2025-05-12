package com.owner.shopping_recommend_service.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.ShoppingUser;
import com.owner.shopping_common.pojo.UserGoodsScore;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.GoodsService;
import com.owner.shopping_common.service.RecommendService;
import com.owner.shopping_common.service.ShoppingUserService;
import com.owner.shopping_recommend_service.mapper.UserGoodsScoreMapper;
import com.owner.shopping_recommend_service.util.ItemCF;
import com.owner.shopping_recommend_service.util.UserCF;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@DubboService
public class RecommendServiceImpl implements RecommendService {
    // 常量定义
    private static final int DEFAULT_RECOMMEND_SIZE = 100;
    private static final int RECOMMEND_EXPIRE_HOURS = 24;
    private static final String RECOMMEND_KEY_PREFIX = "user:recommend:";
    private static final String USER_PROFILES_KEY = "userProfiles";
    private static final String USER_ITEMS_MATRIX = "user_matrix";
    private static final String ITEMS_USER_MATRIX = "items_matrix";

    @Autowired
    private UserGoodsScoreMapper userGoodsScoreMapper;

    @DubboReference
    private ShoppingUserService userService;
    
    @DubboReference
    private GoodsService goodsService;
    
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ItemCF  itemCF;

    @Autowired
    private UserCF userCF;
    /**
     * 获取推荐商品并分页展示
     */
    @Override
    public Page<Goods> searchRecGoods(int page, int size, Long userId) {
        // 验证参数
        if (page < 0 || size <= 0 || userId == null) {
            log.error("无效的参数: page={}, size={}, userId={}", page, size, userId);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
        
        // 获取推荐列表
        List<Long> recommendations = getUserRecommendations(userId);
        
        // 计算分页
        int startIndex = page * size;
        int endIndex = Math.min((page + 1) * size, recommendations.size());
        
        // 检查是否超出推荐列表范围
        if (startIndex >= recommendations.size()) {
            log.error("请求的页码超出推荐列表范围: page={}, size={}, totalSize={}", 
                    page, size, recommendations.size());
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
        
        // 获取当前页的商品ID列表
        List<Long> pageIds = recommendations.subList(startIndex, endIndex);
        
        if (pageIds.isEmpty()) {
            log.error("当前页面没有推荐商品: page={}, size={}", page, size);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
        
        // 根据ID批量查询商品信息
        List<Goods> goodsList = goodsService.findByIds(pageIds);
        
        // 构建分页结果
        Page<Goods> goodsPage = new Page<>();
        goodsPage.setRecords(goodsList);
        goodsPage.setTotal(recommendations.size());
        goodsPage.setSize(goodsList.size());
        goodsPage.setPages(page);
        
        return goodsPage;
    }
    @Override
    public void generateMatrix() {
        Map<Long, Map<Long, Double>> ratings = new HashMap<>();
        Map<Long, Map<Long, Double>> itemMatrix = new HashMap<>();
        //获取所有用户
        List<ShoppingUser> users = userService.getAllUser();
        for (ShoppingUser user : users) {
            Map<Long, Double> map = ratings.getOrDefault(user.getId(), new HashMap<>());
            ratings.put(user.getId(), map);
        }
        //所有商品
        List<Goods> goods = goodsService.findAll();
        //建立用户物品矩阵，物品用户矩阵
        for (Goods good : goods){
            for(ShoppingUser user : users){
                Map<Long, Double> map1 = ratings.getOrDefault(user.getId(), new HashMap<>());
                map1.put(good.getId(), 0.0);
                Map<Long, Double> map2 = itemMatrix.getOrDefault(good.getId(), new HashMap<>());
                map2.put(user.getId(), 0.0);
                ratings.put(user.getId(), map1);
                itemMatrix.put(good.getId(), map2);
            }
        }
        //所有评分
        for (ShoppingUser user : users) {
            log.info("正在处理用户{}的评分数据", user.getId());

            List<UserGoodsScore> list = userGoodsScoreMapper
                    .selectList(new LambdaQueryWrapper<>(UserGoodsScore.class)
                            .eq(UserGoodsScore::getUserid, user.getId()));
            for (UserGoodsScore score : list){
                if (!ratings.containsKey(score.getUserid())||!itemMatrix.containsKey(score.getGoodsid())){
                    continue;
                }
                Map<Long, Double> map1 = ratings.get(score.getUserid());
                Map<Long, Double> map2 = itemMatrix.get(score.getGoodsid());
                map1.put(score.getGoodsid(), score.getScore());
                map2.put(score.getUserid(), score.getScore());
                ratings.put(score.getUserid(), map1);
                itemMatrix.put(score.getGoodsid(), map2);
            }
        }

        //存入缓存，永不过期
        redisTemplate.opsForValue().set(USER_ITEMS_MATRIX, ratings);
        redisTemplate.opsForValue().set(ITEMS_USER_MATRIX, itemMatrix);
    }
    /**
     * 获取用户推荐列表，优先从缓存获取，没有则生成新的
     */
    private List<Long> getUserRecommendations(Long userId) {
        String redisKey = RECOMMEND_KEY_PREFIX + userId;
        
        // 尝试从Redis获取
        @SuppressWarnings("unchecked")
        List<Long> recommendations = (List<Long>) redisTemplate.opsForValue().get(redisKey);
        
        // 如果Redis中没有或不足，则生成新的推荐
        if (recommendations == null || recommendations.isEmpty()) {
            log.info("未找到用户{}的缓存推荐，开始生成新的推荐", userId);
            recommendations = generateRecommendations(userId);
            
            // 缓存推荐结果
            if (!recommendations.isEmpty()) {
                redisTemplate.opsForValue().set(redisKey, recommendations, RECOMMEND_EXPIRE_HOURS, TimeUnit.HOURS);
                log.info("已生成并缓存用户{}的推荐，共{}个商品", userId, recommendations.size());
            }
        } else {
            log.info("从缓存获取用户{}的推荐，共{}个商品", userId, recommendations.size());
        }
        
        return recommendations;
    }
    
    /**
     * 生成用户推荐列表
     */
    private List<Long> generateRecommendations(Long userId) {
        try {
            Map<Long, Map<Long, Double>> ratings = getMatrix(USER_ITEMS_MATRIX);
            Map<Long, Map<Long, Double>> itemMatrix = getMatrix(ITEMS_USER_MATRIX);
            List<Long> recommendByItems = itemCF.recommend(ratings, itemMatrix, userId, DEFAULT_RECOMMEND_SIZE);
            List<Long> recommendByUser = userCF.recommend(ratings, userId, DEFAULT_RECOMMEND_SIZE);
            return recommendByItems.size()>recommendByUser.size() ? recommendByItems : recommendByUser;
        } catch (Exception e) {
            log.error("为用户{}生成推荐时出错: {}", userId, e.getMessage(), e);
            // 返回空列表而不是抛出异常，允许后续处理
            return new ArrayList<>();
        }
    }

    private Map<Long, Map<Long, Double>> getMatrix(String key){
        Map<Long, Map<Long, Double>> matrix = (Map<Long, Map<Long, Double>>) redisTemplate.opsForValue().get(key);
        if (matrix == null ||matrix.isEmpty()){
            generateMatrix();
            matrix = (Map<Long, Map<Long, Double>>) redisTemplate.opsForValue().get(key);
        }
        return matrix;
    }
    /**
     * 刷新所有缓存
     */
    @Override
    public void refreshCache() {
        try {
            log.info("开始刷新推荐系统缓存");
            
            // 删除矩阵缓存
            redisTemplate.delete(USER_ITEMS_MATRIX);
            redisTemplate.delete(ITEMS_USER_MATRIX);
            redisTemplate.delete("user:recommend:28");
            
            // 重新生成矩阵
            generateMatrix();
            
//            // 更新相似度矩阵和用户画像
//            updateSimilarityMatrix();
            
            log.info("推荐系统缓存刷新完成");
        } catch (Exception e) {
            log.error("刷新缓存时出错: {}", e.getMessage(), e);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public void refreshUserRecommendations(Long userId) {
        refreshRecommendation(userId);
        log.info("刷新用户{}的推荐列表完成", userId);

        log.info("开始刷新用户{}的推荐列表", userId);
        getUserRecommendations(userId);
        log.info("刷新用户{}的推荐列表完成", userId);
    }

    private  void refreshRecommendation(Long userId){
        redisTemplate.delete(RECOMMEND_KEY_PREFIX + userId);
    }

    /**
     * 添加用户评论和评分
     */
    @Override
    public void addUserComment(Long userId, Long goodsId, String comment, Double score) {
        // 参数验证
        validateCommentParams(userId, goodsId, score);
        
        try {
            // 检查是否已存在评论
            UserGoodsScore userGoodsScore = findUserComment(userId, goodsId);
            
            if (userGoodsScore == null) {
                // 插入新评论
                UserGoodsScore newScore = new UserGoodsScore(userId, goodsId, comment, score);
                userGoodsScoreMapper.insert(newScore);
                log.info("已添加用户{}对商品{}的评论，评分: {}", userId, goodsId, score);
            } else {
                // 更新现有评论
                userGoodsScore.setComment(comment);
                userGoodsScore.setScore(score);
                updateExistingComment(userGoodsScore);
                log.info("已更新用户{}对商品{}的评论，评分: {}", userId, goodsId, score);
            }
            
            // 更新评分矩阵
            updateMatrix(userId, goodsId, score);
            
            // 刷新用户推荐
            getUserRecommendations(userId);
        } catch (Exception e) {
            log.error("添加用户评论时出错: {}", e.getMessage(), e);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 更新用户评论和评分
     */
    @Override
    public void updateUserComment(Long userId, Long goodsId, String comment, Double score) {
        // 参数验证
        validateCommentParams(userId, goodsId, score);
        
        try {
            // 更新评论
            QueryWrapper<UserGoodsScore> wrapper = new QueryWrapper<>();
            wrapper.eq("userId", userId).eq("goodsId", goodsId);
            UserGoodsScore userGoodsScore = new UserGoodsScore(userId, goodsId, comment, score);
            
            int updated = userGoodsScoreMapper.update(userGoodsScore, wrapper);
            
            if (updated > 0) {
                log.info("已更新用户{}对商品{}的评论，评分: {}", userId, goodsId, score);
                
                // 更新评分矩阵
                updateMatrix(userId, goodsId, score);
                
                // 刷新用户推荐
                getUserRecommendations(userId);
            } else {
                log.warn("未找到用户{}对商品{}的评论，无法更新", userId, goodsId);
                throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
            }
        } catch (Exception e) {
            log.error("更新用户评论时出错: {}", e.getMessage(), e);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 删除用户评论
     */
    @Override
    public void deleteUserComment(Long userId, Long goodsId) {
        if (userId == null || goodsId == null) {
            log.error("删除评论的用户ID或商品ID不能为空");
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
        
        try {
            // 构建删除条件
            QueryWrapper<UserGoodsScore> wrapper = new QueryWrapper<>();
            wrapper.eq("userId", userId).eq("goodsId", goodsId);
            
            int deleted = userGoodsScoreMapper.delete(wrapper);
            
            if (deleted > 0) {
                log.info("已删除用户{}对商品{}的评论", userId, goodsId);
                
                // 更新评分矩阵，删除评论相当于评分为0
                updateMatrix(userId, goodsId, 0.0);
                
                // 刷新用户推荐
                getUserRecommendations(userId);
            } else {
                log.warn("未找到用户{}对商品{}的评论，无法删除", userId, goodsId);
            }
        } catch (Exception e) {
            log.error("删除用户评论时出错: {}", e.getMessage(), e);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 分页获取所有用户评论
     */
    @Override
    public Page<UserGoodsScore> getUserComment(int page, int size) {
        if (page < 0 || size <= 0) {
            log.error("无效的分页参数: page={}, size={}", page, size);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
        
        try {
            return userGoodsScoreMapper.selectPage(new Page<>(page, size), null);
        } catch (Exception e) {
            log.error("获取用户评论分页时出错: {}", e.getMessage(), e);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 获取所有用户评论（内部使用）
     */
    private List<UserGoodsScore> getUserComments() {
        try {
            return userGoodsScoreMapper.selectList(null);
        } catch (Exception e) {
            log.error("获取所有用户评论时出错: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    /**
     * 更新评分矩阵中的单个评分
     */
    @Override
    public void updateMatrix(Long userId, Long goodsId, double score) {
        try {

            Map<Long, Map<Long, Double>> user_matrix = getMatrix(USER_ITEMS_MATRIX);
            Map<Long, Map<Long, Double>> item_matrix = getMatrix(ITEMS_USER_MATRIX);
            // 用户不存在时，创建新用户评分映射
            if (!user_matrix.containsKey(userId)) {
                log.info("矩阵中未找到用户{}，创建新用户评分映射", userId);
                // 复制第一个用户的评分模板并重置为0
                Long templateUserId = user_matrix.keySet().iterator().next();
                Map<Long, Double> template = new HashMap<>(user_matrix.get(templateUserId));
                template.replaceAll((k, v) -> score);
                user_matrix.put(userId, template);
                for (Map<Long, Double> itemMap: item_matrix.values()){
                    itemMap.put(userId,score);
                }
            }
            
            // 商品不存在时，为所有用户添加该商品评分
            if (!item_matrix.containsKey(goodsId)) {
                log.info("矩阵中未找到商品{}，为所有用户添加该商品评分", goodsId);
                Long templateItemId = item_matrix.keySet().iterator().next();
                Map<Long, Double> template = new HashMap<>(item_matrix.get(templateItemId));
                template.replaceAll((k, v) -> score);
                item_matrix.put(goodsId, template);
                for (Map<Long, Double> ratings : user_matrix.values()) {
                    ratings.put(goodsId, score);
                }
            }
            log.info("已更新用户{}对商品{}的评分为{}", userId, goodsId, score);
            
            // 更新Redis缓存
            redisTemplate.opsForValue().set(USER_ITEMS_MATRIX, user_matrix);
            redisTemplate.opsForValue().set(ITEMS_USER_MATRIX, item_matrix);
        } catch (Exception e) {
            log.error("更新评分矩阵时出错: {}", e.getMessage(), e);
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 更新相似度矩阵
     */
    @Override
    public void updateSimilarityMatrix() {
        try {
            log.info("开始更新物品相似度矩阵");
            // 确保用户-物品评分矩阵存在
            Map<Long, Map<Long, Double>> matrix = getMatrix(ITEMS_USER_MATRIX);
            
            // 更新相似度矩阵
            itemCF.calculateItemSimilarities(matrix);
            log.info("物品相似度矩阵更新完成");

        } catch (Exception e) {
            log.error("更新相似度矩阵失败: {}", e.getMessage(), e);
            throw new BusExceptiion(CodeEnum.UPDATE_MATRIX_ERROR);
        }
    }


    /**
     * 验证评论参数
     */
    private void validateCommentParams(Long userId, Long goodsId, Double score) {
        if (userId == null || goodsId == null) {
            log.error("用户ID或商品ID不能为空");
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }

        if (score == null || score < 0 || score > 5) {
            log.error("评分必须在0-5之间");
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 查找用户评论
     */
    private UserGoodsScore findUserComment(Long userId, Long goodsId) {
        LambdaQueryWrapper<UserGoodsScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserGoodsScore::getUserid, userId)
               .eq(UserGoodsScore::getGoodsid, goodsId);
        
        return userGoodsScoreMapper.selectOne(wrapper);
    }

    /**
     * 更新已存在的评论
     */
    private void updateExistingComment(UserGoodsScore userGoodsScore) {
        LambdaQueryWrapper<UserGoodsScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserGoodsScore::getUserid, userGoodsScore.getUserid())
               .eq(UserGoodsScore::getGoodsid, userGoodsScore.getGoodsid());
        
        userGoodsScoreMapper.update(userGoodsScore, wrapper);
    }
}