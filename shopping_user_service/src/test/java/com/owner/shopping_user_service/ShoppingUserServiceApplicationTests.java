package com.owner.shopping_user_service;

import com.owner.shopping_common.util.Md5Util;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
class ShoppingUserServiceApplicationTests {

    @Test
    void contextLoads() {
        System.out.println(Md5Util.encode("123123"));
    }

}
