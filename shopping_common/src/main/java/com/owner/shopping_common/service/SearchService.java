package com.owner.shopping_common.service;

import com.owner.shopping_common.pojo.GoodsDesc;
import com.owner.shopping_common.pojo.GoodsSearchParam;
import com.owner.shopping_common.pojo.GoodsSearchResult;

import java.util.List;

public interface SearchService {
    /**
     * 自动补齐关键字
     * @param keyword 用户输入的待补齐关键字
     * @return 返回补齐后的关键字集合
     */
    List<String> autoSuggest(String keyword);

    /**
     * 根据搜索条件搜索商品
     * @param param 商品搜索条件
     * @return 商品搜索结果
     */
    GoodsSearchResult search(GoodsSearchParam param);

    /**
     * 同步数据到Es
     * @param goodsDesc 商品详情
     */
    void SyncGoodsToES(GoodsDesc goodsDesc);

    /**
     * 商品下架，删除ES中的商品数据
     * @param id 下架的商品id
     */
    void delete(Long id);

    void ScheduledSyncToES();
}
