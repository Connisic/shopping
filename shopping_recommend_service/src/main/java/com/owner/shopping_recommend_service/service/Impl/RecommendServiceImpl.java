package com.owner.shopping_recommend_service.service.Impl;

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
import com.owner.shopping_recommend_service.uitl.CollaborativeFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@DubboService
public class RecommendServiceImpl implements RecommendService {
	@Autowired
	private UserGoodsScoreMapper userGoodsScoreMapper;
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
		Page<Goods> goods = new Page<>();
		List<Long> recommendations = filter.getRecommendations(userId, size);
		List<Long> batchIds = new ArrayList<>();

		for (int i = page * size; i < (page + 1) * size && i < recommendations.size(); i++) {
			batchIds.add(recommendations.get(i));
		}

		if (batchIds.isEmpty()) {
			throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
		}

		List<Goods> allRecommend = goodsService.findByIds(batchIds);
		goods.setRecords(allRecommend);
		goods.setTotal(allRecommend.size());
		goods.setSize(Math.min(size, recommendations.size() - page * size));
		goods.setPages(page);
		return goods;
	}

	@Override
	public void addUserComment(Long userId, Long goodsId, String comment, Double score) {
		//插入用户评论
		userGoodsScoreMapper.insert(new UserGoodsScore(userId, goodsId, comment, score));
		//更新矩阵
		updateMatrix(userId, goodsId, score);

	}

	@Override
	public void updateUserComment(Long userId, Long goodsId, String comment, Double score) {
		//更新条件
		QueryWrapper<UserGoodsScore> wrapper = new QueryWrapper<>();
		wrapper.eq("userId", userId)
				.eq("goodsId", goodsId);
		//更新用户评论
		userGoodsScoreMapper.update(new UserGoodsScore(userId, goodsId, comment, score), wrapper);
		//更新矩阵
		updateMatrix(userId, goodsId, score);

	}

	@Override
	public void deleteUserComment(Long userId, Long goodsId) {
		//删除条件
		QueryWrapper<UserGoodsScore> wrapper = new QueryWrapper<>();
		wrapper.eq("userId", userId)
				.eq("goodsId", goodsId);
		userGoodsScoreMapper.delete(wrapper);
		//更新矩阵
		updateMatrix(userId, goodsId, 0.0);

	}

	@Override
	public Page<UserGoodsScore> getUserComment(int page, int size) {
		return userGoodsScoreMapper.selectPage(new Page<UserGoodsScore>(page, size), null);
	}

	private List<UserGoodsScore> getUserComment() {
		return userGoodsScoreMapper.selectList(null);

	}

	@Override
	public Map<Long, Map<Long, Double>> generateMatrix() {
		List<UserGoodsScore> userComments = getUserComment();
		List<ShoppingUser> userList = userService.getAllUser();
		List<Goods> all = goodsService.findAll();

		Map<Long, Map<Long, Double>> matrix = new ConcurrentHashMap<>(userList.size());

		// 并行处理用户评分矩阵生成
		userList.parallelStream()
				.forEach(user -> {
					Map<Long, Double> map = new ConcurrentHashMap<>();
					all.forEach(goods -> map.put(goods.getId(), 0.0));
					userComments.stream()
							.filter(comment -> comment.getUserid()
									.equals(user.getId()))
							.forEach(comment -> map.put(comment.getGoodsid(), comment.getScore()));
					matrix.put(user.getId(), map);
				});

		redisTemplate.opsForValue()
				.set("matrix", matrix);
		return matrix;
	}

	@Override
	public void updateMatrix(Long userId, Long goodsId, double score) {
		Map<Long, Map<Long, Double>> matrix = (Map<Long, Map<Long, Double>>) redisTemplate.opsForValue()
				.get("matrix");
		if (matrix == null) {
			matrix = generateMatrix();
		}

		if (!matrix.containsKey(userId)) {
			Long fuserId = matrix.keySet()
					.iterator()
					.next();
			Map<Long, Double> m = new ConcurrentHashMap<>(matrix.get(fuserId));
			m.replaceAll((k, v) -> 0.0);
			matrix.put(userId, m);
		}

		Map<Long, Double> map = matrix.get(userId);
		if (!map.containsKey(goodsId)) {
			matrix.values()
					.forEach(userRatings -> userRatings.put(goodsId, 0.0));
		}

		map.put(goodsId, score);
		redisTemplate.opsForValue()
				.set("matrix", matrix);
	}

	@Override
	public void updateSimilarityMatrix() {
		try {
			// 确保用户-物品评分矩阵存在
			Map<Long, Map<Long, Double>> matrix = (Map<Long, Map<Long, Double>>) redisTemplate.opsForValue()
					.get("matrix");
			if (matrix == null || matrix.isEmpty()) {
				matrix = generateMatrix();
			}

			// 更新相似度矩阵
			filter.calculateItemSimilarity();

			// 更新用户画像缓存
			updateUserProfiles();
		} catch (Exception e) {
			log.error("更新相似度矩阵失败: {}", e.getMessage());
			throw new BusExceptiion(CodeEnum.UPDATE_MATRIX_ERROR);
		}
	}

	/**
	 * 更新用户画像缓存
	 */
	private void updateUserProfiles() {
		List<ShoppingUser> users = userService.getAllUser();
		Map<Long, Map<String, Object>> userProfiles = new ConcurrentHashMap<>();

		users.parallelStream()
				.forEach(user -> {
					Map<String, Object> profile = new HashMap<>();

					// 获取用户基本信息
					profile.put("gender", user.getSex());

					// 获取用户行为统计
					List<UserGoodsScore> userComments = userGoodsScoreMapper.selectList(
							new QueryWrapper<UserGoodsScore>()
									.eq("userId", user.getId())
					);

					// 计算用户偏好
					Map<Long, Integer> categoryPreferences = new HashMap<>();
					for (UserGoodsScore comment : userComments) {
						Goods goods = goodsService.findById(comment.getGoodsid());
						if (goods != null && goods.getProductType1Id() != null) {
							categoryPreferences.merge(goods.getProductType1Id(), 1, Integer::sum);
						}
						if (goods != null && goods.getProductType2Id() != null) {
							categoryPreferences.merge(goods.getProductType2Id(), 1, Integer::sum);
						}
						if (goods != null && goods.getProductType3Id() != null) {
							categoryPreferences.merge(goods.getProductType3Id(), 1, Integer::sum);
						}
					}

					// 获取用户最常浏览的类别
					List<Long> preferredCategories = categoryPreferences.entrySet()
							.stream()
							.sorted(Map.Entry.<Long, Integer>comparingByValue()
									.reversed())
							.limit(3)
							.map(Map.Entry::getKey)
							.collect(Collectors.toList());

					profile.put("preferredCategories", preferredCategories);

					// 计算用户活跃度
					profile.put("activityLevel", calculateUserActivity(userComments));

					userProfiles.put(user.getId(), profile);
				});

		// 保存用户画像到Redis
		redisTemplate.opsForValue()
				.set("userProfiles", userProfiles);
	}

	/**
	 * 计算用户活跃度
	 */
	private double calculateUserActivity(List<UserGoodsScore> userComments) {
		if (userComments.isEmpty()) {
			return 0.0;
		}

		// 计算评论数量
		int commentCount = userComments.size();

		// 计算平均评分
		double avgRating = userComments.stream()
				.mapToDouble(UserGoodsScore::getScore)
				.average()
				.orElse(0.0);

		// 计算评论时间跨度
		long timeSpan = calculateTimeSpan(userComments);

		// 综合评分
		return (commentCount * 0.4 + avgRating * 0.3 + (timeSpan > 0 ? 1.0 / timeSpan : 0.0) * 0.3);
	}

	/**
	 * 计算评论时间跨度（天数）
	 */
	private long calculateTimeSpan(List<UserGoodsScore> userComments) {
		if (userComments.isEmpty()) {
			return 0;
		}

		return userComments.stream()
				.mapToLong(comment -> comment.getCreateTime()
						.getTime())
				.max()
				.orElse(0) - userComments.stream()
				.mapToLong(comment -> comment.getCreateTime()
						.getTime())
				.min()
				.orElse(0);
	}

	private double calculateItemScore(Long itemId) {
		Goods goods = goodsService.findById(itemId);
		if (goods == null) {
			return 0.0;
		}

		// 计算综合得分
		double salesScore = goods.getSales() * 0.4;  // 销量权重
		double ratingScore = goods.getRating() * 0.4;  // 评分权重
		double timeScore = calculateTimeScore(goods.getCreateTime()) * 0.2;  // 时间权重

		return salesScore + ratingScore + timeScore;
	}

	private double calculateTimeScore(Date createTime) {
		long now = System.currentTimeMillis();
		long create = createTime.getTime();
		long days = (now - create) / (24 * 60 * 60 * 1000);

		// 使用指数衰减
		return Math.exp(-days / 30.0);  // 30天半衰期
	}

	private Set<Long> getAllItemIds() {
		List<Goods> allGoods = goodsService.findAll();
		return allGoods.stream()
				.map(Goods::getId)
				.collect(Collectors.toSet());
	}
}
