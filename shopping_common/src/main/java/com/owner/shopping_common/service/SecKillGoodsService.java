package com.owner.shopping_common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.SeckillGoods;

public interface SecKillGoodsService {
    //新增秒杀商品
    void add(SeckillGoods seckillGoods);
    //修改秒杀商品
    void update(SeckillGoods seckillGoods);
    //分页查询秒杀商品
    Page<SeckillGoods> search(int page,int size);

}
