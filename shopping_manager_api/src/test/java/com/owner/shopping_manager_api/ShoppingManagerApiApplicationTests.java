package com.owner.shopping_manager_api;

import org.apache.catalina.util.ToStringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class ShoppingManagerApiApplicationTests {
    @Autowired
    private PasswordEncoder encoder;
    @Test
    void contextLoads() {
        String password = encoder.encode("123123");
        System.out.println(password);//$2a$10$HSKqInMg65XZQjel4ySqPu7/w5ITROjOpv4YxoXvKDBNh0wfVgIne
    }


}
