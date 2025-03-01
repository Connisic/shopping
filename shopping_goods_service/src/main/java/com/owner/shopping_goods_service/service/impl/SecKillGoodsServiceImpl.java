package com.owner.shopping_goods_service.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.SeckillGoods;
import com.owner.shopping_common.service.SecKillGoodsService;
import com.owner.shopping_goods_service.mapper.SecKillGoodsMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DubboService
@Transactional
public class SecKillGoodsServiceImpl implements SecKillGoodsService {

    @Autowired
    private SecKillGoodsMapper mapper;
    @Override
    public void add(SeckillGoods seckillGoods) {
        mapper.insert(seckillGoods);
    }

    @Override
    public void update(SeckillGoods seckillGoods) {
        mapper.updateById(seckillGoods);
    }

    @Override
    public Page<SeckillGoods> search(int page, int size) {
        return mapper.selectPage(new Page<>(page,size),null);
    }
}
