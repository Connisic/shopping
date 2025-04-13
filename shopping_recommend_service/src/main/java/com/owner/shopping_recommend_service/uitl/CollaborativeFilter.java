package com.owner.shopping_recommend_service.uitl;

import com.alibaba.nacos.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.GoodsService;
import com.owner.shopping_common.util.RandomUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import jakarta.annotation.PreDestroy;

/**
 * 基于物品的协同过滤推荐系统
 * 实现了基于余弦相似度的物品推荐，并包含多样性控制机制
 */
@Slf4j
@Configuration
public class CollaborativeFilter {
    // 用户-物品评分矩阵：行表示用户，列表示物品，值表示用户对物品的评分
    private Map<Long, Map<Long, Double>> userItemMap;

    // 物品相似度矩阵：存储所有物品之间的相似度计算结果
    private Map<Long, Map<Long, Double>> itemSimilarity;

    // 多样性阈值：控制推荐结果的多样性程度，值越大推荐结果越多样化
    private double diversityThreshold = 0.3;
    //
    @Autowired
    private RedisTemplate redisTemplate;

    // 线程池配置
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    // 线程池配置 - 为IO密集型任务优化的线程池
    private final ThreadPoolExecutor ioThreadPool = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2, // 核心线程数：IO密集型任务通常设置为CPU核心数的2倍
            Runtime.getRuntime().availableProcessors() * 4, // 最大线程数：允许更多线程处理IO等待
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 时间单位
            new LinkedBlockingQueue<>(1000), // 使用有界队列防止OOM
            new ThreadFactoryBuilder().setNameFormat("io-pool-%d").build(), // 自定义线程工厂，便于调试
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者运行
    );

    // 线程池配置 - 为CPU密集型任务优化的线程池
    private final ThreadPoolExecutor cpuThreadPool = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() + 1, // 核心线程数：CPU密集型任务通常设置为CPU核心数+1
            Runtime.getRuntime().availableProcessors() + 1, // 最大线程数：与核心线程数相同，避免频繁上下文切换
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new ThreadFactoryBuilder().setNameFormat("cpu-pool-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // 批处理大小
    private static final int BATCH_SIZE = 100;

    // 稀疏性阈值
    private static final double SPARSITY_THRESHOLD = 0.1;

    // 用户属性权重
    private static final double USER_ATTRIBUTE_WEIGHT = 0.3;
    private static final double ITEM_SIMILARITY_WEIGHT = 0.7;

    @DubboReference
    private GoodsService goodsService;

    /**
     * 初始化推荐系统矩阵，这里如果使用构造函数的话，redisTemplate会为空，因为属性注入在实例化之后
     * 采用@PostConstruct注解，在实例化之后执行函数
     */
    @PostConstruct
    public void init() {
        userItemMap = (Map<Long, Map<Long, Double>>) redisTemplate.opsForValue().get("matrix");
        itemSimilarity = (Map<Long, Map<Long, Double>>) redisTemplate.opsForValue().get("similarity");
        if (itemSimilarity == null) {
            itemSimilarity = calculateItemSimilarity();
        }
    }

    /**
     * 关闭线程池，释放资源
     */
    @PreDestroy
    public void shutdown() {
        // 关闭线程池，等待任务完成
        executorService.shutdown();
        ioThreadPool.shutdown();
        cpuThreadPool.shutdown();
        try {
            // 等待任务完成，最多等待30秒
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!ioThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                ioThreadPool.shutdownNow();
            }
            if (!cpuThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                cpuThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            // 重新标记线程为中断状态
            Thread.currentThread().interrupt();
            // 强制关闭
            executorService.shutdownNow();
            ioThreadPool.shutdownNow();
            cpuThreadPool.shutdownNow();
        }
    }

    /**
     * 使用并行计算优化物品相似度矩阵计算
     */
    public Map<Long, Map<Long, Double>> calculateItemSimilarity() {
        if (userItemMap == null || userItemMap.isEmpty()) {
            userItemMap = calculateItemSimilarity();
        }
        Set<Long> goodsSet = userItemMap.values().iterator().next().keySet();
        Map<Long, Map<Long, Double>> similarity = new ConcurrentHashMap<>(goodsSet.size());

        // 将商品集合分批处理
        List<Long> goodsList = new ArrayList<>(goodsSet);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < goodsList.size(); i += BATCH_SIZE) {
            final int start = i;
            final int end = Math.min(i + BATCH_SIZE, goodsList.size());

            futures.add(CompletableFuture.runAsync(() -> {
                for (int j = start; j < end; j++) {
                    Long item1 = goodsList.get(j);
                    for (Long item2 : goodsSet) {
                        if (item1.equals(item2)) {
                            mapInsert(item1, item2, similarity, 1.0, goodsSet.size());
                            continue;
                        }

                        double sim = calculateCosineSimilarity(
                                getItemVector(item1),
                                getItemVector(item2)
                        );
                        mapInsert(item1, item2, similarity, sim, goodsSet.size());
                        mapInsert(item2, item1, similarity, sim, goodsSet.size());
                    }
                }
            }, cpuThreadPool)); // 使用CPU密集型线程池，因为相似度计算是计算密集型任务
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 异步保存到Redis
        CompletableFuture.runAsync(() -> {
            redisTemplate.opsForValue().set("similarity", similarity);
        }, ioThreadPool).join(); // 使用IO密集型线程池，因为Redis操作是IO密集型任务

        this.itemSimilarity = similarity;  // 更新当前实例的相似度矩阵
        return similarity;
    }

    public void mapInsert(Long i, Long j, Map<Long, Map<Long, Double>> map, Double d, int goodsSize) {
        if (map.containsKey(i)) {
            Map<Long, Double> m = map.get(i);
            m.put(j, d);
        } else {
            Map<Long, Double> m = new HashMap<>(goodsSize);
            m.put(j, d);
            map.put(i, m);
        }
    }

    /**
     * 优化的物品向量获取方法
     */
    private double[] getItemVector(Long itemId) {
        return userItemMap.values().stream()
                .map(userRatings -> userRatings.getOrDefault(itemId, 0.0))
                .mapToDouble(Double::doubleValue)
                .toArray();
    }

    /**
     * 优化的余弦相似度计算
     */
    private double calculateCosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 优化的推荐生成方法，包含稀疏性处理和冷启动策略
     */
    public List<Long> getRecommendations(Long userId, int numItems) {
        // 检查矩阵是否为空
        if (userItemMap == null || userItemMap.isEmpty()) {
            return getDefaultRecommendations(numItems);
        }

        Map<Long, Double> userRatings = userItemMap.get(userId);

        // 检查数据稀疏性
        double sparsity = calculateSparsity(userRatings);

        if (userRatings == null || sparsity > SPARSITY_THRESHOLD) {
            // 使用混合推荐策略
            return getHybridRecommendations(userId, numItems);
        }

        // 使用协同过滤推荐
        return getCollaborativeRecommendations(userRatings, numItems);
    }

    /**
     * 默认推荐策略，当矩阵为空时使用
     * 基于商品的基本属性（如销量、评分等）进行推荐
     */
    private List<Long> getDefaultRecommendations(int numItems) {
        // 从Redis获取商品热度数据
        Map<Long, Double> itemPopularity = (Map<Long, Double>) redisTemplate.opsForValue().get("item:popularity");

        if (itemPopularity == null || itemPopularity.isEmpty()) {
            // 如果Redis中没有热度数据，从数据库获取
            itemPopularity = generateItemPopularity();
        }

        // 按热度排序并返回推荐结果
        return itemPopularity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(numItems)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 生成商品热度数据
     * 基于商品的销量、评分、上架时间等维度计算综合热度得分
     */
    private Map<Long, Double> generateItemPopularity() {
        Map<Long, Double> popularity = new HashMap<>();

        // 获取所有商品数据
        List<Goods> allGoods = goodsService.findAll();

        // 计算每个商品的热度得分
        for (Goods goods : allGoods) {
            double score = calculateItemScore(goods);
            if (goods.getSales() == null) {
                goods.setSales(RandomUtil.getRandom(100, 1000));
            }
            if (goods.getRating() == null) {
                goods.setRating(RandomUtil.getRandom(3.0, 5.0));
            }
            if (goods.getCreateTime() == null) {
                goods.setCreateTime(new Date());
            }
            goodsService.update(goods);
            popularity.put(goods.getId(), score);
        }
        // 将热度数据缓存到Redis，设置24小时过期
        redisTemplate.opsForHash().putAll("item:popularity", popularity);
        redisTemplate.expire("item:popularity", 24, TimeUnit.HOURS);

        return popularity;
    }

    /**
     * 计算商品综合得分
     * 考虑销量(40%)、评分(40%)和时间(20%)三个维度
     */
    private double calculateItemScore(Goods goods) {
        if (goods == null) return 0.0;

        // 销量得分（归一化到0-1）
        double salesScore = normalizeSales(goods.getSales());

        // 评分得分（归一化到0-1）
        double ratingScore = normalizeRating(goods.getRating());

        // 时间得分（使用指数衰减）
        double timeScore = calculateTimeScore(goods.getCreateTime());

        // 加权平均计算最终得分
        return salesScore * 0.4 + ratingScore * 0.4 + timeScore * 0.2;
    }

    /**
     * 销量归一化处理
     * 使用对数函数处理长尾分布
     */
    private double normalizeSales(Integer sales) {
        if (sales == null || sales <= 0) return 6.0;
        // 使用对数函数处理长尾分布
        return Math.log1p(sales) / Math.log1p(getMaxSales());
    }

    /**
     * 获取最大销量
     */
    private Integer getMaxSales() {
        return goodsService.findAll().stream()
                .map(Goods::getSales)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(1);
    }

    /**
     * 评分归一化处理
     */
    private double normalizeRating(Double rating) {
        if (rating == null || rating <= 0) return 8.0;
        // 假设评分范围是1-5
        return (rating - 1) / 4.0;
    }

    /**
     * 计算时间得分
     * 使用指数衰减函数，设置30天半衰期
     */
    private double calculateTimeScore(Date createTime) {
        if (createTime == null) return 4.0;

        long now = System.currentTimeMillis();
        long create = createTime.getTime();
        long days = (now - create) / (24 * 60 * 60 * 1000);

        // 使用指数衰减
        return Math.exp(-days / 30.0);  // 30天半衰期
    }

    /**
     * 获取所有商品ID
     */
    private Set<Long> getAllItemIds() {
        // TODO: 从商品服务获取所有商品ID
        // 这里暂时返回空集合
        return new HashSet<>();
    }

    /**
     * 计算用户评分矩阵的稀疏性
     */
    private double calculateSparsity(Map<Long, Double> userRatings) {
        if (userRatings == null || userRatings.isEmpty()) {
            return 1.0;
        }

        long totalItems = userItemMap.values().iterator().next().size();
        long ratedItems = userRatings.values().stream()
                .filter(rating -> rating > 0)
                .count();

        return 1.0 - (double) ratedItems / totalItems;
    }

    /**
     * 混合推荐策略，结合协同过滤和基于用户属性的推荐
     */
    private List<Long> getHybridRecommendations(Long userId, int numItems) {
        // 获取基于用户属性的推荐
        List<Long> attributeBasedRecommendations = getAttributeBasedRecommendations(userId, numItems);

        // 获取基于物品相似度的推荐
        List<Long> similarityBasedRecommendations = getSimilarityBasedRecommendations(userId, numItems);

        // 合并推荐结果
        Set<Long> finalRecommendations = new LinkedHashSet<>();

        // 根据权重合并推荐结果
        int attributeCount = (int) (numItems * USER_ATTRIBUTE_WEIGHT);
        int similarityCount = numItems - attributeCount;

        // 添加基于属性的推荐
        attributeBasedRecommendations.stream()
                .limit(attributeCount)
                .forEach(finalRecommendations::add);

        // 添加基于相似度的推荐
        similarityBasedRecommendations.stream()
                .filter(item -> !finalRecommendations.contains(item))
                .limit(similarityCount)
                .forEach(finalRecommendations::add);

        return new ArrayList<>(finalRecommendations);
    }

    /**
     * 基于用户属性的推荐
     */
    private List<Long> getAttributeBasedRecommendations(Long userId, int numItems) {
        // 获取用户画像（这里需要从用户服务获取）
        Map<String, Object> userProfile = getUserProfile(userId);

        // 基于用户画像的推荐逻辑
        return getPopularItemsByCategory(userProfile, numItems);
    }

    /**
     * 基于物品相似度的推荐
     */
    private List<Long> getSimilarityBasedRecommendations(Long userId, int numItems) {
        Map<Long, Double> userRatings = userItemMap.get(userId);
        if (userRatings == null) {
            return getPopularItems(numItems, null);
        }

        Map<Long, Double> scores = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        // 并行计算预测评分
        for (Long goodsId : userRatings.keySet()) {
            futures.add(executorService.submit(() -> {
                double score = calculatePredictedRating(userRatings, goodsId);
                scores.put(goodsId, score);
            }));
        }

        // 等待所有预测完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return getDiverseRecommendations(scores, numItems);
    }

    /**
     * 获取热门商品推荐
     */
    private List<Long> getPopularItems(int numItems, List<Long> categoryTypes) {
        Map<Long, Double> itemPopularity = new HashMap<>();

        // 计算商品热度
        for (Map<Long, Double> userRatings : userItemMap.values()) {
            userRatings.forEach((itemId, rating) -> {
                if (rating > 0) {
                    itemPopularity.merge(itemId, 1.0, Double::sum);
                }
            });
        }

        // 应用时间衰减因子
        applyTimeDecay(itemPopularity);

        List<Long> allResult = itemPopularity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return genResultByCategory(allResult, categoryTypes, numItems);
    }

    private List<Long> genResultByCategory(List<Long> allResult, List<Long> categoryTypes, int numItems) {
        if (categoryTypes == null){
            return allResult.stream()
                    .limit(numItems)
                    .collect(Collectors.toList());
        }
        return categoryTypes.stream()
                .flatMap(categoryType -> allResult.stream()
                        .filter(itemId -> {
                            if (categoryType==null)
                                return false;
                            Goods goods = goodsService.findById(itemId);
                            if (categoryType.equals(goods.getProductType1Id()))
                                return true;
                            if (categoryType.equals(goods.getProductType2Id()))
                                return true;
                            return categoryType.equals(goods.getProductType3Id());
                        })
                        .limit(numItems ))
                .collect(Collectors.toList());
    }

    /**
     * 应用时间衰减因子
     */
    private void applyTimeDecay(Map<Long, Double> itemPopularity) {
        // 批量获取所有商品的年龄数据
        Map<Long, Double> itemAges = getItemAgesBatch(itemPopularity.keySet());

        // 应用衰减因子
        itemPopularity.replaceAll((itemId, score) -> {
            // 获取商品年龄，如果不存在则默认为0
            double age = itemAges.getOrDefault(itemId, 0.0);
            // 使用线性衰减模型：一年的商品衰减到原始分数的0
            return score * Math.max(0, 1.0 - (age / 365.0));
        });
    }

    /**
     * 获取商品年龄（天数）
     */
    private double getItemAge(Long itemId) {
        // 尝试从Redis缓存中获取商品年龄数据
        String cacheKey = "item:age:" + itemId;
        Double cachedAge = (Double) redisTemplate.opsForValue().get(cacheKey);

        if (cachedAge != null) {
            return cachedAge;
        }

        // 缓存未命中，获取商品详情
        Goods goods = goodsService.findById(itemId);
        if (goods == null) {
            return 0.0;
        }

        return (double) (new Date().getTime() - goods.getCreateTime().getTime()) / (1000 * 60 * 60 * 24.0);
    }

    /**
     * 批量获取商品年龄数据
     *
     * @param itemIds 商品ID集合
     * @return 商品ID到年龄的映射
     */
    private Map<Long, Double> getItemAgesBatch(Collection<Long> itemIds) {
        Map<Long, Double> ageMap = new ConcurrentHashMap<>();
        if (itemIds == null || itemIds.isEmpty()) {
            return ageMap;
        }
        // 先从Redis批量获取缓存数据
        List<String> cacheKeys = itemIds.stream()
                .map(id -> "item:age:" + id)
                .collect(Collectors.toList());

        List<Object> cachedAges = redisTemplate.opsForValue().multiGet(cacheKeys);

        // 处理缓存命中的结果
        List<Long> missedIds = new ArrayList<>();
        int index = 0;
        for (Long itemId : itemIds) {
            Object cachedAge = cachedAges.get(index++);
            if (cachedAge != null) {
                ageMap.put(itemId, (Double) cachedAge);
            } else {
                missedIds.add(itemId);
            }
        }

        // 如果存在缓存未命中的商品，异步获取
        if (!missedIds.isEmpty()) {
            // 分批处理，每批100个
            int batchSize = 100;
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 0; i < missedIds.size(); i += batchSize) {
                List<Long> batch = missedIds.subList(i, Math.min(i + batchSize, missedIds.size()));

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // 批量获取商品信息
                        List<Goods> goodsList = goodsService.findByIds(batch);
                        Map<String, Object> cacheData = new HashMap<>();

                        for (Goods goods : goodsList) {
                            if (goods != null && goods.getCreateTime() != null) {
                                long now = System.currentTimeMillis();
                                long createTime = goods.getCreateTime().getTime();
                                double ageInDays = (now - createTime) / (24.0 * 60 * 60 * 1000);

                                ageMap.put(goods.getId(), ageInDays);
                                cacheData.put("item:age:" + goods.getId(), ageInDays);
                            } else {
                                // 对于无效数据，设置为0
                                if (goods != null) {
                                    ageMap.put(goods.getId(), 0.0);
                                    cacheData.put("item:age:" + goods.getId(), 0.0);
                                }
                            }
                        }

                        // 批量写入缓存
                        if (!cacheData.isEmpty()) {
                            for (Map.Entry<String, Object> entry : cacheData.entrySet()) {
                                redisTemplate.opsForValue().set(entry.getKey(), entry.getValue(), 24, TimeUnit.HOURS);
                            }
                        }
                    } catch (Exception e) {
                        log.error("批量获取商品年龄数据失败: {}", e.getMessage());
                        throw new BusExceptiion(CodeEnum.BATCH_QUERY_ERROR);
                    }
                }, ioThreadPool);

                futures.add(future);
            }

            // 等待所有异步任务完成
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("等待异步任务完成失败: {}", e.getMessage());
                throw new BusExceptiion(CodeEnum.BATCH_QUERY_ERROR);
            }
        }

        return ageMap;
    }

    /**
     * 获取用户画像
     */
    private Map<String, Object> getUserProfile(Long userId) {
        Map<Long, Map<String, Object>> userProfiles = (Map<Long, Map<String, Object>>) redisTemplate.opsForValue().get("userProfiles");
        if (userProfiles != null && userProfiles.containsKey(userId)) {
            return userProfiles.get(userId);
        }
        return new HashMap<>();
    }

    /**
     * 基于用户画像获取热门商品
     */
    private List<Long> getPopularItemsByCategory(Map<String, Object> userProfile, int numItems) {
        List<Long> preferredCategories = (List<Long>) userProfile.get("preferredCategories");
        return getPopularItems(numItems, preferredCategories);
    }

    /**
     * 使用多样性控制算法选择推荐项
     * 通过综合考虑物品评分和多样性得分，选择最终推荐列表
     *
     * @param scores   物品的预测评分数组
     * @param numItems 需要推荐的物品数量
     * @return 考虑多样性后的推荐物品ID列表
     */
    private List<Long> getDiverseRecommendations(Map<Long, Double> scores, int numItems) {
        List<Long> recommendations = new ArrayList<>();
        Set<Long> selected = new HashSet<>(scores.size());
        //boolean[] selected = new boolean[scores.length];

        // 首先选择得分最高的项目作为起点
        Long firstItem = getMaxScoreIndex(scores, selected);
        recommendations.add(firstItem);
        selected.add(firstItem);

        // 迭代选择剩余推荐项
        while (recommendations.size() < numItems) {
            Long nextItem = 0L;
            double maxScore = Double.NEGATIVE_INFINITY;

            // 遍历所有未选择的物品
            for (Long i : scores.keySet()) {
                if (selected.contains(i)) continue;

                // 计算当前物品与已选物品的多样性得分
                double diversityScore = calculateDiversityScore(i, recommendations);
                // 将原始评分与多样性得分结合
                double combinedScore = scores.get(i) * (1 + diversityScore);

                // 更新最高得分的物品
                if (combinedScore > maxScore) {
                    maxScore = combinedScore;
                    nextItem = i;
                }
            }

            // 如果没有找到合适的物品，结束推荐
            if (nextItem == 0L) break;
            recommendations.add(nextItem);
            selected.add(nextItem);
        }

        return recommendations;
    }

    /**
     * 计算物品的多样性得分
     * 基于物品与已选择物品的平均相似度，返回差异度得分
     *
     * @param goodsId       待评估物品ID
     * @param selectedItems 已选择的物品ID列表
     * @return 多样性得分（0-1之间，越大表示差异性越大）
     */
    private double calculateDiversityScore(Long goodsId, List<Long> selectedItems) {
        if (selectedItems.isEmpty()) return 1.0;

        // 计算与已选物品的总相似度
        double totalSimilarity = 0.0;
        for (Long selectedItem : selectedItems) {
            totalSimilarity += itemSimilarity.get(goodsId).get(selectedItem);
        }

        // 计算平均差异度（1减去平均相似度）
        return 1.0 - (totalSimilarity / selectedItems.size());
    }

    /**
     * 计算用户对特定物品的预测评分
     * 使用加权平均方法：Σ(相似度 * 评分) / Σ|相似度|
     *
     * @param userRatings 用户的历史评分向量
     * @param goodsId     待预测商品ID
     * @return 预测的评分值
     */
    private double calculatePredictedRating(Map<Long, Double> userRatings, Long goodsId) {
        double weightedSum = 0.0;
        double similaritySum = 0.0;

        // 遍历用户的所有评分
        for (Long i : userRatings.keySet()) {
            // 只考虑用户已评分的物品
            if (userRatings.get(i) > 0) {
                weightedSum += itemSimilarity.get(goodsId).get(i) * userRatings.get(i);
                similaritySum += Math.abs(itemSimilarity.get(goodsId).get(i));
            }
        }
        // 避免除零错误
        return similaritySum == 0.0 ? 0.0 : weightedSum / similaritySum;
    }

    /**
     * 获取评分数组中最高分数的索引
     *
     * @param scores   评分数组
     * @param selected 已选择的物品标记数组
     * @return 未被选择的物品中评分最高的索引
     */
    private Long getMaxScoreIndex(Map<Long, Double> scores, Set<Long> selected) {
        Long maxIndex = 0L;
        double maxScore = Double.NEGATIVE_INFINITY;

        // 遍历所有评分，找出未被选择的最高分物品
        for (Long i : scores.keySet()) {
            if (!selected.contains(i) && scores.get(i) > maxScore) {
                maxScore = scores.get(i);
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    /**
     * 使用协同过滤进行推荐
     *
     * @param userRatings 用户的历史评分
     * @param numItems    需要推荐的物品数量
     * @return 推荐的物品ID列表
     */
    private List<Long> getCollaborativeRecommendations(Map<Long, Double> userRatings, int numItems) {
        // 计算用户对所有未评分物品的预测评分
        Map<Long, Double> predictedRatings = new ConcurrentHashMap<>();

        // 获取所有商品ID
        Set<Long> allItems = userItemMap.values().iterator().next().keySet();

        // 找出用户未评分的商品
        Set<Long> unratedItems = allItems.stream()
                .filter(itemId -> !userRatings.containsKey(itemId) || userRatings.get(itemId) == 0)
                .collect(Collectors.toSet());

        // 使用CompletableFuture并行计算每个未评分商品的预测评分
        List<CompletableFuture<Void>> futures = unratedItems.stream()
                .map(itemId -> CompletableFuture.runAsync(() -> {
                    // 计算评分预测是IO密集型任务（可能涉及数据库或缓存读取）
                    double predictedRating = calculatePredictedRating(userRatings, itemId);
                    predictedRatings.put(itemId, predictedRating);
                }, ioThreadPool))
                .collect(Collectors.toList());

        // 等待所有异步任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 使用多样性控制算法选择最终推荐项（CPU密集型任务）
        CompletableFuture<List<Long>> diverseRecommendationsFuture =
                CompletableFuture.supplyAsync(() -> getDiverseRecommendations(predictedRatings, numItems), cpuThreadPool);

        return diverseRecommendationsFuture.join();
    }
}