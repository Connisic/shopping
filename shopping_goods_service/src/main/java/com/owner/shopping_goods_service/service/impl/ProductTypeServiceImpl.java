package com.owner.shopping_goods_service.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.ProductType;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.ProductTypeService;
import com.owner.shopping_goods_service.mapper.ProductTypeMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
@DubboService
@Transactional
public class ProductTypeServiceImpl implements ProductTypeService {
    @Autowired
    private ProductTypeMapper mapper;

    @Override
    public void add(ProductType productType) {
        //根据父类型id查询父类型，
        ProductType parentType = mapper.selectById(productType.getParentId());
        // 通过父类型的level级别来设置当前类型级别
        if (parentType==null)//没有父类型即为一级类型
            productType.setLevel(1);
        else if(parentType.getLevel()<3){
            productType.setLevel(parentType.getLevel()+1);
        }else{
            throw new BusExceptiion(CodeEnum.INSERT_PRODUCT_TYPE_ERROR);
        }
        mapper.insert(productType);
    }

    @Override
    public void update(ProductType productType) {
        //根据父类型id查询父类型，
        ProductType parentType = mapper.selectById(productType.getParentId());
        // 通过父类型的level级别来设置当前类型级别
        if (parentType==null)//没有父类型即为一级类型
            productType.setLevel(1);
        else if(parentType.getLevel()<3){
            productType.setLevel(parentType.getLevel()+1);
        }else{
            throw new BusExceptiion(CodeEnum.INSERT_PRODUCT_TYPE_ERROR);
        }

        mapper.updateById(productType);
    }

    @Override
    public void delete(Long id) {
        //1查询该类型的子类型
        QueryWrapper<ProductType> wrapper = new QueryWrapper<>();
        wrapper.eq("parentId",id);
        List<ProductType> list = mapper.selectList(wrapper);
        //2如果该类型有子类型，删除失败
        if(CollectionUtils.isNotEmpty(list)){
            throw new BusExceptiion(CodeEnum.DELETE_PRODUCT_TYPE_ERROR);
        }
        //3没有子类型，直接删除
        mapper.deleteById(id);

    }

    @Override
    public ProductType findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Page<ProductType> search(ProductType productType, int page, int size) {
        QueryWrapper<ProductType> wrapper = new QueryWrapper<>();
        //类型名不为空
        if (productType!=null){
            //类型名不为空
            if(StringUtils.hasText(productType.getName()))
                wrapper.like("name",productType.getName());
            //上级id不为空
            if(productType.getParentId()!=null)
                wrapper.eq("parentId",productType.getParentId());
        }
        return mapper.selectPage(new Page<>(page,size),wrapper);
    }

    @Override
    public List<ProductType> findProductType(ProductType productType) {
        QueryWrapper<ProductType> wrapper = new QueryWrapper<>();
        //类型名不为空
        if (productType!=null){
            //类型名不为空
            if(StringUtils.hasText(productType.getName()))
                wrapper.like("name",productType.getName());
            //上级id不为空
            if(productType.getParentId()!=null)
                wrapper.eq("parentId",productType.getParentId());
        }
        return mapper.selectList(wrapper);
    }
}
