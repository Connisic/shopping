package com.owner.shopping_common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.UserGoodsScore;

import java.util.List;
import java.util.Map;

//商品推荐服务
public interface RecommendService {
    //分页查找推荐商品
    Page<Goods> searchRecGoods(int page, int size, Long userId);
    //添加用户评价
    void addUserComment(Long userId,Long goodsId,String comment,Double score);
    //更新用户评价
    void updateUserComment(Long userId,Long goodsId,String comment,Double score);
    //删除用户评价
    void deleteUserComment(Long userId,Long goodsId);
    //获取用户评价
    List<UserGoodsScore> getUserComment();
    //生成用户-商品-评分矩阵，行为用户id，列为商品 id，值为评分
    Map<Long,Map<Long,Double>> generateMatrix();
    //更新redis保存的矩阵
    void updateMatrix(Long userId,Long goodsId,double score);

}
