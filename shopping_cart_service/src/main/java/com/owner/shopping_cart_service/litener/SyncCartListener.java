package com.owner.shopping_cart_service.litener;

import com.owner.shopping_common.pojo.CartGoods;
import com.owner.shopping_common.service.CartService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "sync_cart_queue",consumerGroup = "sync_cart_group")
public class SyncCartListener implements RocketMQListener<CartGoods> {

    @Autowired
    private CartService cartService;
    @Override
    public void onMessage(CartGoods cartGoods) {
        System.out.println("同步购物车商品信息");
        cartService.refreshCartGoods(cartGoods);
    }
}
