package com.owner.shopping_user_customer_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class ShoppingUserCustomerApiApplicationTests {

    @Test
    void contextLoads() {
        List<Integer> list=new ArrayList<>();
        list.add(1);
        list.forEach(i -> list.remove(i));
    }

}
