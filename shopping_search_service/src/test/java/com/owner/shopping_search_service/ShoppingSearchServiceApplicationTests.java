package com.owner.shopping_search_service;

import com.owner.shopping_common.pojo.GoodsDesc;
import com.owner.shopping_common.service.GoodsService;
import com.owner.shopping_common.service.SearchService;
import com.owner.shopping_search_service.service.Impl.SearchServiceImpl;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ShoppingSearchServiceApplicationTests {
    @Autowired
    private SearchServiceImpl service;

    @DubboReference
    private GoodsService goodsService;
    @Test
    void contextLoads() {
        List<String> analyze = service.analyze("我爱百战程序员", "ik_pinyin");
                System.out.println("我爱柏芝");
        System.out.println(analyze);
    }

    @Test
    void testSyncGoodsToES(){
        List<GoodsDesc> all = goodsService.findAll();
        for (GoodsDesc goodsDesc : all) {
            if (goodsDesc.getIsMarketable()==true){
                service.SyncGoodsToES(goodsDesc);
            }
        }
    }

}
