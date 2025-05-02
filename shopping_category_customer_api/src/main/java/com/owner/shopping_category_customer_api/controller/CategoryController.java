package com.owner.shopping_category_customer_api.controller;

import com.owner.shopping_common.pojo.Category;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 广告相关的控制器
 */
@RestController
@RequestMapping("/user/category")
public class CategoryController {

    @DubboReference
    private CategoryService categoryService;

    @GetMapping("/all")
    public BaseResult<List<Category>> findAll(){
        List<Category> all = categoryService.findAll();
        return BaseResult.ok(all);
    }
}
