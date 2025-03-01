package com.owner.shopping_user_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
class ShoppingUserServiceApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;
    @Test
    void contextLoads() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("registerkey1",1);

    }

}
