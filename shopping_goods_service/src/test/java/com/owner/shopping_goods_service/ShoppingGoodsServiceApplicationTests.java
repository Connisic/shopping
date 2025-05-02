package com.owner.shopping_goods_service;

import com.owner.shopping_common.pojo.GoodsDesc;
import com.owner.shopping_common.service.GoodsService;
import com.owner.shopping_goods_service.mapper.GoodsMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ShoppingGoodsServiceApplicationTests {
    @Autowired
    private GoodsService service;
    @Autowired
    private GoodsMapper mapper;
    @Test
    void contextLoads() {
        System.out.println(mapper.findDesc(149187842868151L));
    }

}
