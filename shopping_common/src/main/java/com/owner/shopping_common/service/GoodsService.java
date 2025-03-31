package com.owner.shopping_common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.GoodsDesc;
import org.apache.catalina.Service;

import java.util.ArrayList;
import java.util.List;

public interface GoodsService  {

    //新增商品
    void add(Goods goods);
    //新增商品集合
    void addAll(ArrayList<Goods> goodsList);
    //修改商品
    void update (Goods goods);
    //根据id查询商品详情
    Goods findById(Long id);
    //上架/下架商品
    void putAway(Long id,Boolean isMarcketable);
    //分页查询商品
    Page<Goods> search(Goods goods/* 分页查询条件 */,int page,int size);
    //查询所有商品详情
    List<GoodsDesc> findAllDesc();

    List<Goods> findAll();
    //根据id查询商品详情
    GoodsDesc findDesc(Long id);
    //根据id删除商品
    void delete(Long id);
}
