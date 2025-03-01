package com.owner.shopping_category_service.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_category_service.mapper.CategoryMapper;
import com.owner.shopping_common.pojo.Category;
import com.owner.shopping_common.service.CategoryService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@DubboService
@Transactional
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper mapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void add(Category category) {
        mapper.insert(category);
        refreshRedisCategry();
    }

    @Override
    public void update(Category category) {
        mapper.updateById(category);
        refreshRedisCategry();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Category category = mapper.selectById(id);
        category.setStatus(status);
        mapper.updateById(category);
        refreshRedisCategry();
    }

    @Override
    public void delete(Long[] ids) {
        mapper.deleteBatchIds(Arrays.asList(ids));
        refreshRedisCategry();
    }

    @Override
    public Category findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Page<Category> search(int page, int size) {
        return mapper.selectPage(new Page<>(page,size),null);
    }

    @Override
    public List<Category> findAll() {
        //1从redis中查询广告
        ListOperations<String,Category> listOperations = redisTemplate.opsForList();
        List<Category> categoryList = listOperations.range("categories", 0, -1);
        if(categoryList!=null&&categoryList.size()>0){
            //2如果查询到了，直接返回
            System.out.println("从redis中查询数据！");
            return categoryList;
        }else{
            //3redis中没有数据，从数据库查询
            QueryWrapper<Category> wrapper = new QueryWrapper<>();
            wrapper.eq("status",1);
            List<Category> categories = mapper.selectList(wrapper);
            //4同步到redis中
            redisTemplate.opsForList().leftPushAll("categories",categories);
            return categories;
        }

    }

    public void refreshRedisCategry(){
        //从数据库中查询所有广告
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("status",1);
        List<Category> categories = mapper.selectList(wrapper);
        //向redis插入list数据时，不能为空而且长度大于零才进行更新操作
        if (categories!=null&&categories.size()>0){
            //删除redsi原有的广告数据
            redisTemplate.delete("categories");
            //将查询出来的广告数据插入redis
            redisTemplate.opsForList().leftPushAll("categories",categories);
        }

    }
}
