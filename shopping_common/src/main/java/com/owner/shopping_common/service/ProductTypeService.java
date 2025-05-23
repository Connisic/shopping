package com.owner.shopping_common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.ProductType;

import java.util.List;

public interface ProductTypeService {
    //新增商品类型
    void add(ProductType productType);
    //修改商品类型
    void update(ProductType productType);
    //删除商品类型
    void delete(Long id);
    //根据id查询商品类型
    ProductType findById(Long id);
    //分页查询商品类型
    Page<ProductType> search(ProductType productType,int page,int size);
    //根据条件查询商品类型
    List<ProductType> findProductType(ProductType productType);
}
