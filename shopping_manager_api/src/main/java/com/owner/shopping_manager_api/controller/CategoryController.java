package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Category;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.CategoryService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/category")
public class CategoryController {
    @DubboReference
    private CategoryService categoryService;

    /**
     * 新增广告
     * @param category 广告
     * @return 执行结果
     */
    @PostMapping("/add")
    public BaseResult add(@RequestBody Category category){
        categoryService.add(category);
        return BaseResult.ok();
    }

    /**
     * 修改广告
     * @param category 要修改的广告
     * @return 执行结果
     */
    @PutMapping("/update")
    public BaseResult update(@RequestBody Category category){
        categoryService.update(category);
        return BaseResult.ok();
    }

    /**
     * 修改广告状态
     * @param id 广告id
     * @param status 广告状态 0：未启用，1：启用
     * @return 执行结果
     */
    @PutMapping("/updateStatus")
    public BaseResult updateStatus(Long id,Integer status){
        categoryService.updateStatus(id, status);
        return BaseResult.ok();
    }

    /**
     * 删除广告
     * @param ids 广告id集合
     * @return 执行结果
     */
    @DeleteMapping("/delete")
    public BaseResult delete(Long [] ids){
        categoryService.delete(ids);
        return BaseResult.ok();
    }

    /**
     * 根据id查询结果
     * @param id 广告id
     * @return 查询结果
     */
    @GetMapping("/findById")
    public BaseResult<Category> findById(Long id){
        Category category = categoryService.findById(id);
        return BaseResult.ok(category);
    }

    /**
     * 分页查询广告
     * @param page 页码
     * @param size 每页条数
     * @return 查询结果
     */
    @GetMapping("/search")
    public BaseResult<Page<Category>> search(int page,int size){
        Page<Category> categoryPage = categoryService.search(page, size);
        return BaseResult.ok(categoryPage);
    }
}
