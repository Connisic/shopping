package com.owner.shopping_recommend_service.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.GoodsDesc;
import com.owner.shopping_common.pojo.ShoppingUser;
import com.owner.shopping_common.pojo.UserGoodsScore;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.GoodsService;
import com.owner.shopping_common.service.RecommendService;
import com.owner.shopping_common.service.ShoppingUserService;
import com.owner.shopping_recommend_service.mapper.GoodsMapper;
import com.owner.shopping_recommend_service.mapper.UserGoodsScoreMapper;
import com.owner.shopping_recommend_service.uitl.CollaborativeFilter;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DubboService
public class RecommendServiceImpl implements RecommendService {
    @Autowired
    private UserGoodsScoreMapper userGoodsScoreMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    //注入协同过滤工具类
    @Autowired
    private CollaborativeFilter filter;
    //注入用户服务
    @DubboReference
    private ShoppingUserService userService;
    //注入商品服务
    @DubboReference
    private GoodsService goodsService;
    //redis
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Override
    public Page<Goods> searchRecGoods(int page, int size, Long userId) {
        Page<Goods> goods=new Page<>();
        List<Long> recommendations = filter.getRecommendations(userId, size);
        List<Long> BatchIds=new ArrayList<>();
        for (int i=page*size;i<(page+1)*size&&i<recommendations.size();i++){
            BatchIds.add(recommendations.get(i));
        }
        if (BatchIds.size()==0)
            throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        List<Goods> allRecommend = goodsMapper.selectBatchIds(BatchIds);
        goods.setRecords(allRecommend);
        goods.setTotal(allRecommend.size());
        goods.setSize(Math.min(size,recommendations.size()-page*size));
        goods.setPages(page);
        return goods;
    }

    @Override
    public void addUserComment(Long userId, Long goodsId, String comment, Double score) {
        //插入用户评论
        userGoodsScoreMapper.insert(new UserGoodsScore(userId,goodsId,comment,score));
        //更新矩阵
        updateMatrix(userId,goodsId,score);

    }

    @Override
    public void updateUserComment(Long userId, Long goodsId, String comment, Double score) {
        //更新条件
        QueryWrapper<UserGoodsScore> wrapper = new QueryWrapper<>();
        wrapper.eq("userId",userId).eq("goodsId",goodsId);
        //更新用户评论
        userGoodsScoreMapper.update(new UserGoodsScore(userId,goodsId,comment,score),wrapper);
        //更新矩阵
        updateMatrix(userId,goodsId,score);

    }

    @Override
    public void deleteUserComment(Long userId, Long goodsId) {
        //删除条件
        QueryWrapper<UserGoodsScore> wrapper = new QueryWrapper<>();
        wrapper.eq("userId",userId).eq("goodsId",goodsId);
        userGoodsScoreMapper.delete(wrapper);
        //更新矩阵
        updateMatrix(userId,goodsId,0.0);

    }

    @Override
    public List<UserGoodsScore> getUserComment() {
        List<UserGoodsScore> allComments = userGoodsScoreMapper.selectList(new QueryWrapper<>());
        return allComments;
    }

    @Override
    public Map<Long, Map<Long, Double>> generateMatrix() {
        //获取评论集合
        List<UserGoodsScore> userComments = getUserComment();
        //获取用户集合
        List<ShoppingUser> userList = userService.getAllUser();
        //获取商品集合
        List<GoodsDesc> all = goodsService.findAll();
        Map<Long,Map<Long,Double>> matrix=new HashMap<>(userList.size());
        //生成矩阵
        for(ShoppingUser user:userList){
            Map<Long,Double> map=new HashMap<>();
            for (GoodsDesc goods:all){
                map.put(goods.getId(),0.0);
            }
            for(UserGoodsScore comment:userComments){
                if (comment.getUserid()==user.getId()){
                    map.put(comment.getGoodsid(),comment.getScore());
                }
            }
            matrix.put(user.getId(),map);
        }
        redisTemplate.opsForValue().set("matrix",matrix);
        return matrix;
    }

    @Override
    public void updateMatrix(Long userId, Long goodsId, double score) {
        Map<Long,Map<Long,Double>> matrix=(Map<Long,Map<Long,Double>>)redisTemplate.opsForValue().get("matrix");
        if( !matrix.containsKey(userId)){
            Long fuserId = matrix.keySet().stream().findFirst().get();
            Map<Long,Double> m = new HashMap(matrix.get(fuserId));
            //新用户，创建新行并赋初值0.0
            for(Long i:m.keySet()){
                m.put(i,0.0);
            }
            matrix.put(userId,m);
        }
        Map<Long, Double> map= matrix.get(userId);
        if( !map.containsKey(goodsId)){
            //新商品，创建新列赋初值0.0
            for(Long i:matrix.keySet()){
                matrix.get(i).put(goodsId,0.0);
            }
        }
        map.put(goodsId,score);
        redisTemplate.delete("matrix");
        redisTemplate.opsForValue().set("matrix",matrix);
    }
}
