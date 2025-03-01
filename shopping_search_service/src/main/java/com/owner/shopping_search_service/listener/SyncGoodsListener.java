package com.owner.shopping_search_service.listener;

import com.owner.shopping_common.pojo.GoodsDesc;
import com.owner.shopping_common.service.SearchService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "sync_goods_queue",consumerGroup = "sync_goods_group")
public class SyncGoodsListener implements RocketMQListener<GoodsDesc> {
    @Autowired
    private SearchService searchService;

    @Override
    public void onMessage(GoodsDesc goodsDesc) {
        System.out.println("同步ES商品信息");
        //同步ES数据
        searchService.SyncGoodsToES(goodsDesc);
    }
}
