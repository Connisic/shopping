package com.owner.shopping_seckill_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class ShoppingSeckillServiceApplicationTests {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Test
    void contextLoads() {
        String password = passwordEncoder.encode("123123");
        System.out.println(password);//$2a$10$HSKqInMg65XZQjel4ySqPu7/w5ITROjOpv4YxoXvKDBNh0wfVgIne
    }

}
