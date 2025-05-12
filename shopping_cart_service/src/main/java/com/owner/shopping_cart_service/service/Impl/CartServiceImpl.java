package com.owner.shopping_cart_service.service.Impl;

import com.owner.shopping_common.dto.RecommendDto;
import com.owner.shopping_common.pojo.CartGoods;
import com.owner.shopping_common.service.CartService;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *购物车服务接口实现类
 */
@DubboService
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    private static final String CART_KEY="cartList";
    private static final String RFRESH_RECOMMEND_QUEUE="refresh_recommend_queue";
    @Override
    public void addCart(Long userId, CartGoods cartGoods) {
        //1 根据userId获取购物车商品集合
        List<CartGoods> cartList = findCartList(userId);
        //1.1判断新增商品是否已经添加过
        for (CartGoods goods : cartList) {
            if (cartGoods.getGoodId().equals(goods.getGoodId())){//如果添加过则增加该商品数量，
                goods.setNum(goods.getNum()+cartGoods.getNum());
                redisTemplate.boundHashOps(CART_KEY).put(userId,cartList);
                return;
            }
        }
        //否则直接添加到购物车商品集合
        cartList.add(cartGoods);
        RecommendDto dto = new RecommendDto();
        dto.setGoodsId(cartGoods.getGoodId());
        dto.setUserId(userId);
        List<RecommendDto> list = new ArrayList<>();
        list.add(dto);
        rocketMQTemplate.syncSend(RFRESH_RECOMMEND_QUEUE,list);
        redisTemplate.boundHashOps(CART_KEY).put(userId,cartList);
    }

    @Override
    public void handleCart(Long userId, Long goodId, Integer num) {
        //1、获取购物车列表
        List<CartGoods> cartList = findCartList(userId);
        //2、遍历列表找到商品
        for (CartGoods cartGoods : cartList) {        //3、改变商品数量
            if (goodId.equals(cartGoods.getGoodId())){
                cartGoods.setNum(num);
            }
        }
        //4、将新的购物车列表保存到redis
        redisTemplate.boundHashOps(CART_KEY).put(userId,cartList);
    }

    @Override
    public void deleteCartOption(Long userId, Long goodId) {
        //1、获取购物车列表
        List<CartGoods> cartList = findCartList(userId);
        //2、遍历列表找到商品
        for (CartGoods cartGoods : cartList) {        //3、从redis中删除
            if (goodId.equals(cartGoods.getGoodId())){
                cartList.remove(cartGoods);
                break;
            }
        }
        //4、将新的购物车列表保存到redis
        redisTemplate.boundHashOps(CART_KEY).put(userId,cartList);
    }

    @Override
    public List<CartGoods> findCartList(Long userId) {
        Object cartList = redisTemplate.boundHashOps(CART_KEY).get(userId);
        if (cartList==null){
            return new ArrayList<CartGoods>();
        }else{
            return (List<CartGoods>) cartList;
        }
    }

    @Override
    public void refreshCartGoods(CartGoods cartGoods) {
        //获取所有用户的购物车
        BoundHashOperations cartList = redisTemplate.boundHashOps(CART_KEY);
        Map<Long,List<CartGoods>> allCartGoods = cartList.entries();
        Collection<List<CartGoods>> values = allCartGoods.values();
        //遍历所有用户的购物车
        for (List<CartGoods> goodsList : values) {
            //遍历每个购物车的商品
            for (CartGoods goods : goodsList) {
                //如果遍历的是要更新的商品，就更新数据
                if (cartGoods.getGoodId().equals(goods.getGoodId())){
                    goods.setGoodsName(cartGoods.getGoodsName());
                    goods.setHeaderPic(cartGoods.getHeaderPic());
                    goods.setPrice(cartGoods.getPrice());
                    break;
                }
            }
        }

        //删除原有数据,将改变后的购物车放入redis
        redisTemplate.delete(CART_KEY);
        redisTemplate.boundHashOps(CART_KEY).putAll(allCartGoods);
    }

    @Override
    public void deleteCartGoods(Long goodId) {
        //获取所有用户的购物车
        BoundHashOperations cartList = redisTemplate.boundHashOps(CART_KEY);
        Map<Long,List<CartGoods>> allCartGoods = cartList.entries();
        Collection<List<CartGoods>> values = allCartGoods.values();
        //遍历所有用户的购物车
        for (List<CartGoods> goodsList : values) {
            //遍历每个购物车的商品
            for (CartGoods goods : goodsList) {
                //如果遍历的是要删除的商品，删除！
                if(goodId.equals(goods.getGoodId())){
                    goodsList.remove(goods);
                    break;
                }
            }
        }

        //删除原有数据,将删除后的购物车放入redis
        redisTemplate.delete(CART_KEY);
        redisTemplate.boundHashOps(CART_KEY).putAll(allCartGoods);
    }
}
