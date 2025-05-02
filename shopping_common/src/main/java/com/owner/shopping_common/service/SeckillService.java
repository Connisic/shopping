package com.owner.shopping_common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.pojo.SeckillGoods;

//秒杀商品接口
public interface SeckillService {

    /**
     * 分页查询秒杀商品
     * @param page 页码
     * @param size 每页条数
     * @return 分页查询结果
     */
    Page<SeckillGoods> findPageByRedis(int page,int size);

    //根据id查询秒杀商品详情

    /**
     * @param goodsId 秒杀商品对应的商品id
     * @return
     */
    SeckillGoods findDescByRedis(Long goodsId);

    //生成秒杀商品订单
    /**
     * @param orders 待生成的订单
     * @return 返回生成好的订单
     */
    Orders createOrder(Orders orders);

    //支付订单

    /**
     * @param orderId 订单id
     * @return
     */
    Orders pay(String orderId);

    Orders findOrder(String id);

    void addSeckillGoodsToRedis(SeckillGoods seckillGoods);
}
