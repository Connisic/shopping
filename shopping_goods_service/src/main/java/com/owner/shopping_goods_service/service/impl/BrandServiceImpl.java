package com.owner.shopping_goods_service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Brand;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.BrandService;
import com.owner.shopping_goods_service.mapper.BrandMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@DubboService
@Transactional
public class BrandServiceImpl implements BrandService {
    @Autowired
    private BrandMapper mapper;

    @Override
    public Brand findById(Integer id) {
        //if (id==0){
        //    int i=1/0;
        //}else if(id==1){
        //    throw new BusExceptiion(CodeEnum.SYSTEM_ERROR);
        //}
        return mapper.selectById(id);
    }

    @Override
    public List<Brand> findAll() {
        return mapper.selectList(null);
    }

    @Override
    public void add(Brand brand) {
        mapper.insert(brand);
    }

    @Override
    public void update(Brand brand) {
        mapper.updateById(brand);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Page<Brand> search(Brand brand, int page, int size) {
        QueryWrapper<Brand> wrapper = new QueryWrapper<>();
        if (brand!=null&& StringUtils.hasText(brand.getName()))
            wrapper.like("name",brand.getName());
        Page<Brand> brandPage = mapper.selectPage(new Page<>(page, size), wrapper);
        return brandPage;
    }
}
