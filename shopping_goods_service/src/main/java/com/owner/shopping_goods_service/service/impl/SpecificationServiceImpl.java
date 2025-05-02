package com.owner.shopping_goods_service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Specification;
import com.owner.shopping_common.pojo.SpecificationOption;
import com.owner.shopping_common.pojo.SpecificationOptions;
import com.owner.shopping_common.service.SpecificationService;
import com.owner.shopping_goods_service.mapper.SpecificationMapper;
import com.owner.shopping_goods_service.mapper.SpecificationOptionMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
@DubboService
@Transactional
public class SpecificationServiceImpl implements SpecificationService {
    @Autowired
    private SpecificationMapper mapper;
    @Autowired
    private SpecificationOptionMapper optionMapper;
    @Override
    public void add(Specification specification) {
        mapper.insert(specification);
    }

    @Override
    public void update(Specification specification) {
        mapper.updateById(specification);
    }

    @Override
    public void delete(Long[] ids) {

        for (Long id:ids){
            //1根据specid删除商品规格项
            QueryWrapper<SpecificationOption> wrapper = new QueryWrapper<>();
            wrapper.eq("specId",id);
            optionMapper.delete(wrapper);
            //2根据id删除商品规格
            mapper.deleteById(id);
        }
    }

    @Override
    public Specification findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Page<Specification> search(int page, int size) {
        return mapper.selectPage(new Page<>(page,size),null);
    }

    @Override
    public List<Specification> findByProductTypeId(Long id) {
        return mapper.findByProductTypeId(id);
    }

    @Override
    public void addOption(SpecificationOptions specificationOptions) {
        //拿到规格id
        Long specId = specificationOptions.getSpecId();
        //拿到规格项数组
        String[] optionName = specificationOptions.getOptionName();
        for (String o: optionName){
            //构建规格项对象
            SpecificationOption option = new SpecificationOption();
            option.setOptionName(o);
            option.setSpecId(specId);
            //存到数据库中
            optionMapper.insert(option);
        }
    }

    @Override
    public void deleteOption(Long[] ids) {
        optionMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
