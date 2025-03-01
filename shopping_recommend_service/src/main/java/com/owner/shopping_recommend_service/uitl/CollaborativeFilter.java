package com.owner.shopping_recommend_service.uitl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.*;

/**
 * 基于物品的协同过滤推荐系统
 * 实现了基于余弦相似度的物品推荐，并包含多样性控制机制
 */
@Configuration
public class CollaborativeFilter {
    // 用户-物品评分矩阵：行表示用户，列表示物品，值表示用户对物品的评分
    //private double[][] userItemMatrix;
    private Map<Long,Map<Long,Double>> userItemMap;
    // 物品相似度矩阵：存储所有物品之间的相似度计算结果
    private Map<Long,Map<Long,Double>> itemSimilarity;
    
    // 多样性阈值：控制推荐结果的多样性程度，值越大推荐结果越多样化
    private double diversityThreshold = 0.3;
    //
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 初始化推荐系统矩阵，这里如果使用构造函数的话，redisTemplate会为空，因为属性注入在实例化之后
     * 采用@PostConstruct注解，在实例化之后执行函数
     */
    @PostConstruct
    public void init() {
        userItemMap = (Map<Long, Map<Long, Double>>) redisTemplate.opsForValue().get("matrix");
        itemSimilarity = (Map<Long, Map<Long, Double>>) redisTemplate.opsForValue().get("similarity");
        if (itemSimilarity==null){
            itemSimilarity=calculateItemSimilarity();
        }
    }

    /**
     * 计算物品间的相似度矩阵
     * @return 物品相似度矩阵
     */
    private Map<Long, Map<Long, Double>> calculateItemSimilarity() {
        //用户集合
        Set<Long> userSet = userItemMap.keySet();
        Long userId = userSet.stream().findFirst().get();
        //商品集合
        Set<Long> goodsSet = userItemMap.get(userId).keySet();
        int goodsSize = goodsSet.size();
        //int itemCount = userItemMatrix[0].length;
        Map<Long,Map<Long,Double>> similarity=new HashMap<>(goodsSize);
        // 遍历所有物品对，计算它们之间的相似度
        for(Long i:goodsSet){
            for(Long j:goodsSet){
                // 物品与自身的相似度为1
                if (i == j) {
                    mapInsert(i,j,similarity,1.0,goodsSize);
                    continue;
                }

                // 计算两个物品向量的余弦相似度
                double sim = calculateCosineSimilarity(getItemVector(i,userSet.size()), getItemVector(j,userSet.size()));
                // 由于相似度矩阵是对称的，同时设置[i][j]和[j][i]
                mapInsert(i,j,similarity,sim,goodsSize);
                mapInsert(j,i,similarity,sim,goodsSize);
                this.itemSimilarity=similarity;
            }
        }
        redisTemplate.delete("similarity");
        redisTemplate.opsForValue().set("similarity",similarity);
        return similarity;
    }

    public void mapInsert(Long i,Long j,Map<Long,Map<Long,Double>> map,Double d,int goodsSize){
        if (map.containsKey(i)){
            Map<Long, Double> m = map.get(i);
            m.put(j,d);
        }else{
            Map<Long, Double> m= new HashMap<>(goodsSize);
            m.put(j,d);
            map.put(i,m);
        }
    }

    /**
     * 为指定用户生成推荐列表
     * @param userId 用户ID
     * @param numItems 需要推荐的物品数量
     * @return 推荐的物品ID列表
     */
    public List<Long> getRecommendations(Long userId, int numItems) {
        //double[] userRatings = userItemMatrix[userId];
        //double[] scores = new double[userItemMatrix[0].length];
        Map<Long, Double> userRatings = userItemMap.get(userId);
        Map<Long, Double> scores=new HashMap<>(userRatings.size());
        // 为每个物品计算预测评分

        for(Long goodsId:userRatings.keySet()){
            scores.put(goodsId,calculatePredictedRating(userRatings,goodsId));
        }

        // 使用多样性控制算法选择最终推荐项
        return getDiverseRecommendations(scores, Math.min(numItems,userRatings.size()));
    }

    /**
     * 使用多样性控制算法选择推荐项
     * 通过综合考虑物品评分和多样性得分，选择最终的推荐列表
     * @param scores 物品的预测评分数组
     * @param numItems 需要推荐的物品数量
     * @return 考虑多样性后的推荐物品ID列表
     */
    private List<Long> getDiverseRecommendations(Map<Long, Double> scores, int numItems) {
        List<Long> recommendations = new ArrayList<>();
        Set<Long> selected=new HashSet<>(scores.size());
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
            for (Long i:scores.keySet()) {
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
     * @param goodsId 待评估物品ID
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
     * 计算两个向量间的余弦相似度
     * cos(θ) = (A·B)/(||A||·||B||)
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度值（-1到1之间，1表示方向完全相同）
     */
    private double calculateCosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        // 计算点积和向量范数
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        // 处理零向量的情况
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        // 返回余弦相似度
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 获取物品的评分向量
     * @param itemId 物品ID
     * @return 该物品的用户评分向量
     */
    private double[] getItemVector(Long itemId,Integer userSize) {
        double[] itemVector = new double[userSize];
        // 获取所有用户对该物品的评分
        int j=0;
        for (Long i:userItemMap.keySet()){
            itemVector[j++]=userItemMap.get(i).get(itemId);
        }
        return itemVector;
    }

    /**
     * 计算用户对特定物品的预测评分
     * 使用加权平均方法：Σ(相似度 * 评分) / Σ|相似度|
     * @param userRatings 用户的历史评分向量
     * @param goodsId 待预测商品ID
     * @return 预测的评分值
     */
    private double calculatePredictedRating(Map<Long,Double> userRatings, Long goodsId) {
        double weightedSum = 0.0;
        double similaritySum = 0.0;

        // 遍历用户的所有评分
        for(Long i:userRatings.keySet()){
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
     * @param scores 评分数组
     * @param selected 已选择的物品标记数组
     * @return 未被选择的物品中评分最高的索引
     */
    private Long getMaxScoreIndex(Map<Long, Double> scores,Set<Long> selected) {
        Long maxIndex = 0L;
        double maxScore = Double.NEGATIVE_INFINITY;

        // 遍历所有评分，找出未被选择的最高分物品
        for(Long i:scores.keySet()){
            if ( !selected.contains(i) && scores.get(i) > maxScore) {
                maxScore = scores.get(i);
                maxIndex = i;
            }
        }

        return maxIndex;
    }
    //public static void main(Long[] args) {
    //    // 示例用户-物品评分矩阵
    //    //double[][] userItemMatrix = {
    //    //        {5.0, 3.0, 0.0, 1.0},
    //    //        {4.0, 0.0, 0.0, 1.0},
    //    //        {1.0, 1.0, 0.0, 5.0},
    //    //        {1.0, 0.0, 0.0, 4.0},
    //    //        {0.0, 1.0, 5.0, 4.0},
    //    //};
    //    Map<Integer,Map<Long,Double>>userItemMatrix=new HashMap<>(5);
    //    //Map<Long,Double> m1=new HashMap<>();
    //    //m1.put("s1",5.0);
    //    //m1.put("s2",3.0);
    //    //m1.put("s3",0.0);
    //    //m1.put("s4",1.0);
    //    //userItemMatrix.put(1,m1);
    //    //Map<Long,Double> m2=new HashMap<>();
    //    //m2.put("s1",4.0);
    //    //m2.put("s2",0.0);
    //    //m2.put("s3",0.0);
    //    //m2.put("s4",1.0);
    //    //userItemMatrix.put(2,m2);
    //    //Map<Long,Double> m3=new HashMap<>();
    //    //m3.put("s1",1.0);
    //    //m3.put("s2",1.0);
    //    //m3.put("s3",0.0);
    //    //m3.put("s4",5.0);
    //    //userItemMatrix.put(3,m3);
    //    //Map<Long,Double> m4=new HashMap<>();
    //    //m4.put("s1",1.0);
    //    //m4.put("s2",0.0);
    //    //m4.put("s3",0.0);
    //    //m4.put("s4",4.0);
    //    //userItemMatrix.put(4,m4);
    //    //Map<Long,Double> m5=new HashMap<>();
    //    //m5.put("s1",0.0);
    //    //m5.put("s2",1.0);
    //    //m5.put("s3",5.0);
    //    //m5.put("s4",4.0);
    //    //userItemMatrix.put(5,m5);
    //    CollaborativeFilter cf = new CollaborativeFilter();
    //
    //    // 为用户0推荐2个物品
    //    List<Long> recommendations = cf.getRecommendations(0L, 3);
    //    System.out.println("推荐的物品ID: " + recommendations);
    //}
}