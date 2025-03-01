package com.owner.shopping_seckill_customer_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.pojo.SeckillGoods;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.OrderService;
import com.owner.shopping_common.service.SeckillService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import javax.sql.rowset.BaseRowSet;

@RestController
@RequestMapping("/user/seckillGoods")
public class SeckillGoodsController {
    @DubboReference
    private SeckillService service;

    @DubboReference
    private OrderService orderService;

    /**
     * 用户分页查询秒杀商品
     * @param page 页码
     * @param size 每页条数
     * @return 返回分页查询结果
     */
    @GetMapping("/findPage")
    public BaseResult<Page<SeckillGoods>> findPage(int page, int size){
        Page<SeckillGoods> seckillGoodsPage = service.findPageByRedis(page, size);
        return BaseResult.ok(seckillGoodsPage);
    }

    /**
     * 根据商品id查询秒杀商品详情
     * @param id 商品id
     * @return 查询结果
     */
    @GetMapping("/findById")
    public BaseResult<SeckillGoods> findById(Long id){
        SeckillGoods seckillGoods = service.findDescByRedis(id);
        return BaseResult.ok(seckillGoods);
    }

    /**
     * 生成订单功能
     * @param orders 前端传来的待生成订单
     * @param userId 订单对应的用户id
     * @return 返回订单详情
     */
    @PostMapping("/add")
    public BaseResult<Orders> add(@RequestBody Orders orders,@RequestHeader Long userId){
        orders.setUserId(userId);
        Orders order = service.createOrder(orders);
        return BaseResult.ok(order);
    }

    /**
     * 根据订单id从redis查询订单详情
     * @param id 订单id
     * @return 返回订单详情
     */
    @GetMapping("/findOrder")
    public BaseResult<Orders> findOrder(String id){
        Orders order = service.findOrder(id);
        return BaseResult.ok(order);
    }

    @GetMapping("/pay")
    public BaseResult pay(String id){
        Orders orders = service.pay(id);
        //支付成功保存到数据库
        orderService.add(orders);
        return BaseResult.ok();
    }
}
