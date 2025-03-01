package com.owner.shopping_recommend_service;

import com.owner.shopping_common.service.RecommendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ShoppingRecommendServiceApplicationTests {
    @Autowired
    private RecommendService recommendService;
    @Test
    void contextLoads() {
        //recommendService.generateMatrix();
        //recommendService.updateMatrix(1L,1L,1.0);
        recommendService.searchRecGoods(0,15,28L);
    }

}
