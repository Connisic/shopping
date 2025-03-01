package com.owner.shopping_seckill_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class ShoppingSeckillServiceApplicationTests {
    @Autowired
    private RedisTemplate redisTemplate;
    @Test
    void contextLoads() {
        //redisTemplate.boundHashOps("seckillGoods").delete("149187842868005");
        System.out.println("我爱你");
    }

}
