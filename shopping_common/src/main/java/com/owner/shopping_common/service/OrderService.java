package com.owner.shopping_common.service;

import com.owner.shopping_common.pojo.Orders;

import java.util.List;

public interface OrderService {
    //生成订单
    Orders add(Orders orders);
    //修改订单
    void update(Orders orders);
    //根据订单id查询订单
    Orders findById(String id);
    //查询用户所有订单
    List<Orders > findUserOrders(Long userId,Integer status);
}
