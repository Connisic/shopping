package com.owner.shopping_order_service.service.Impl;

import com.owner.shopping_common.pojo.CartGoods;
import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.service.OrderService;
import com.owner.shopping_order_service.mapper.CartGoodsMapper;
import com.owner.shopping_order_service.mapper.OrdersMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
@DubboService
@Transactional
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private CartGoodsMapper cartGoodsMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private final String EXPIRE_ORDERS_QUEUE = "expire_orders_queue";

    @Override
    public Orders add(Orders orders) {
        //设置订单未付款
        if (orders.getStatus()==null){
            orders.setStatus(1);
        }
        //订单创建时间
        orders.setCreateTime(new Date());
        //计算订单总价，遍历订单所有商品
        BigDecimal sum = BigDecimal.ZERO;
        List<CartGoods> cartGoods = orders.getCartGoods();
        for (CartGoods cartGood : cartGoods) {

            //数量
            BigDecimal num = new BigDecimal(cartGood.getNum());
            //单价
            BigDecimal price = cartGood.getPrice();

            //数量*单价
            BigDecimal multiply = num.multiply(price);
            sum = sum.add(multiply);
        }
        orders.setPayment(sum);
        //保存订单
        ordersMapper.insert(orders);

        for (CartGoods cartGood : cartGoods) {
            //保存订单商品信息
            cartGood.setOrderId(orders.getId());
            cartGoodsMapper.insert(cartGood);
        }

        //发送延时消息 ,30分钟后判断是否支付
        //延时等级1到18分别表示 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
        rocketMQTemplate.syncSend(EXPIRE_ORDERS_QUEUE, MessageBuilder.withPayload(orders.getId()).build(),15000,14);

        return orders;
    }

    @Override
    public void update(Orders orders) {
        ordersMapper.updateById(orders);
    }

    @Override
    public Orders findById(String id) {
        Orders orders = ordersMapper.findById(id);
        return orders;
    }

    @Override
    public List<Orders> findUserOrders(Long userId, Integer status) {
        return ordersMapper.findOrdersByUserIdAndStatus(userId,status);
    }
}
