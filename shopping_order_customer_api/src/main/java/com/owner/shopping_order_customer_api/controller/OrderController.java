package com.owner.shopping_order_customer_api.controller;

import com.owner.shopping_common.pojo.CartGoods;
import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.CartService;
import com.owner.shopping_common.service.OrderService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/orders")
public class OrderController {
    @DubboReference
    private OrderService orderService;

    @DubboReference
    private CartService cartService;

    /**
     * 生成订单
     * @param orders 待生成订单
     * @return 返回生成好的订单
     */
    @PostMapping("/add")
    public BaseResult<Orders> add(@RequestBody Orders orders, @RequestHeader Long userId){
        //生成订单
        orders.setUserId(userId);
        Orders result = orderService.add(orders);
        //删除redis购物车商品信息
        List<CartGoods> cartGoods = orders.getCartGoods();
        for (CartGoods cartGood : cartGoods) {
            cartService.deleteCartOption(userId,cartGood.getGoodId());
        }
        return BaseResult.ok(result);
    }

    /**
     * 根据id查询订单详情
     * @param id 订单id
     * @return 返回查询结果
     */
    @GetMapping("/findById")
    public BaseResult<Orders> findById(String id){
        Orders orders = orderService.findById(id);
        return BaseResult.ok(orders);
    }

    /**
     * 根据用户id查询所有订单
     * @param userId 用户id
     * @param status 订单状态：1，未付款 2，已付款 3，未发货 4，已发货 5，交易成功 6，交易关闭 7，待评价
     * @return
     */
    @GetMapping("/findUserOrders")
    public BaseResult<List<Orders>> findUserOrders(@RequestHeader Long userId,Integer status){
        List<Orders> orderes = orderService.findUserOrders(userId, status);
        return BaseResult.ok(orderes);
    }

}
