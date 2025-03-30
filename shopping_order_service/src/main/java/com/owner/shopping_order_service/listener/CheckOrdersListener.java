package com.owner.shopping_order_service.listener;

import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
//检查订单过期的消费者

@Service
@RocketMQMessageListener(topic = "expire_orders_queue",consumerGroup = "expire_orders_group")
public class CheckOrdersListener implements RocketMQListener<String> {
    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(String orderId) {
        //查询订单
        Orders orders = orderService.findById(orderId);
        if (orders==null){
            return;
        }
        //判断订单是否支付
        if (orders.getStatus()==1){
            //订单关闭交易
            orders.setStatus(6); //6:交易关闭
            //设置订单过期时间
            orders.setExpire(new Date());
            orderService.update(orders);//更新数据库
        }
    }
}
