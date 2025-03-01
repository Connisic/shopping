package com.owner.shopping_order_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.shopping_common.pojo.Orders;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrdersMapper extends BaseMapper<Orders> {
    //查询订单详情
    Orders findById(String id);
    //根据用户id查询所有订单
    List<Orders> findOrdersByUserIdAndStatus(@Param("userId") Long userId,@Param("status") Integer status);
}
