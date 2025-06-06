package com.owner.shopping_cart_customer_api.controller;

import com.owner.shopping_common.pojo.CartGoods;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.CartService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/user/cart")
public class CartController {
    @DubboReference
    private CartService cartService;

    /**
     * 根据用户userid从redis中查询购物车列表
     * @param userId 令牌token中携带的userId
     * @return 返回购物车列表
     */
    @GetMapping("/findCartList")
    public BaseResult<List<CartGoods>> findCartList(@RequestHeader Long userId){
        List<CartGoods> cartList = cartService.findCartList(userId);
        return BaseResult.ok(cartList);
    }

    /**
     * 新增购物车商品到redis
     * @param userId 令牌token中携带的userId
     * @param cartGoods 购物车商品
     * @return 执行结果
     */
    @PostMapping("/addCart")
    public BaseResult addCart(@RequestHeader Long userId,@RequestBody CartGoods cartGoods){
        cartService.addCart(userId,cartGoods);
        return BaseResult.ok();
    }

    /**
     * 修改购物车信息
     * @param userId 令牌token中携带的userId
     * @param goodId 商品id
     * @param num 购物车商品数量
     * @return 执行结果
     */
    @PutMapping("/handleCart")
    public BaseResult handleCart(@RequestHeader Long userId,Long goodId,Integer num){
        cartService.handleCart(userId,goodId,num);
        return BaseResult.ok();
    }

    /**
     * 删除购物车商品信息
     * @param userId 令牌token中携带的userId
     * @param goodId 商品id
     * @return 执行结果
     */
    @DeleteMapping("/deleteCart")
    public BaseResult deleteCart(@RequestHeader Long userId,Long goodId){
        cartService.deleteCartOption(userId,goodId);
        return BaseResult.ok();
    }

}
