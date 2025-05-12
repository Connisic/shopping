package com.owner.shopping_common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.UserGoodsScore;

import java.util.List;
import java.util.Map;

public interface RecommendService {
    // 分页查询推荐商品
    Page<Goods> searchRecGoods(int page, int size, Long userId);

    // 添加用户评论
    void addUserComment(Long userId, Long goodsId, String comment, Double score);

    // 更新用户评论
    void updateUserComment(Long userId, Long goodsId, String comment, Double score);

    // 删除用户评论
    void deleteUserComment(Long userId, Long goodsId);

    // 获取用户评论
    Page<UserGoodsScore> getUserComment(int page,int size);

    // 生成用户-物品评分矩阵
    void generateMatrix();

    // 更新用户-物品评分矩阵
    void updateMatrix(Long userId, Long goodsId, double score);

    // 更新相似度矩阵
    void updateSimilarityMatrix();

    void refreshCache();

    void refreshUserRecommendations(Long userId);
}