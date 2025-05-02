package com.owner.shopping_seckill_service.redis;

import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.pojo.SeckillGoods;
import com.owner.shopping_common.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * redis监听类继承KeyExpirationEventMessageListener
 */
@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillService service;
    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    /**
     * 监听过期订单，获取订单对象，回退库存
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        //获取失效的key，订单id
        String expiredKey = message.toString();
        Orders orders = (Orders) redisTemplate.opsForValue().get(expiredKey + "_copy");

        //删除过期订单副本
        redisTemplate.delete(expiredKey+"_copy");
        //商品id
        Long goodId = orders.getCartGoods().get(0).getGoodId();
        //商品数量
        Integer num = orders.getCartGoods().get(0).getNum();
        SeckillGoods seckillGoods = service.findDescByRedis(goodId);//查询秒杀商品
        //回退库存
        seckillGoods.setStockCount(seckillGoods.getStockCount()+num);

        //更新redis数据
        redisTemplate.boundHashOps("seckillGoods").put(seckillGoods.getGoodsId(),seckillGoods);


    }
}
