package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.pojo.Brand;
import com.owner.shopping_common.service.BrandService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brand")
public class BrandController {
    //远程注入
    @DubboReference
    private BrandService service;

    /**
     * 根据id查询品牌
     * @param id 品牌id
     * @return 品牌结果
     */
    @GetMapping("/findById")
    public BaseResult<Brand> findById(Integer id){
        Brand brand = service.findById(id);
        return BaseResult.ok(brand);
    }

    /**
     * 查询所有品牌，用于新建商品选择品牌类型
     * @return 返回所有品牌
     */
    @GetMapping("/all")
    public BaseResult<List<Brand>> findAll(){
        List<Brand> all = service.findAll();
        return BaseResult.ok(all);
    }

    /**
     *新增品牌
     * @param brand 要新增的品牌
     * @return 执行结果
     */
    @PostMapping("/add")
    public BaseResult add(@RequestBody Brand brand){
        service.add(brand);
        return BaseResult.ok();
    }

    /**
     * 根据id删除品牌
     * @param id 品牌id
     * @return 执行结果
     */
    @DeleteMapping("/delete")
    public BaseResult delete(Long id){
        service.delete(id);
        return BaseResult.ok();
    }

    /**
     * 修改品牌
     * @param brand 修改后的品牌
     * @return 执行结果
     */
    @PutMapping("/update")
    public BaseResult update(@RequestBody Brand brand){
        service.update(brand);
        return BaseResult.ok();
    }

    /**
     * 分页查询+模糊查询品牌
     * @param brand 模糊查询条件
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果/*-*
     */
    @GetMapping("/search")
    public BaseResult<Page<Brand>> search(Brand brand,int page,int size){
        Page<Brand> brandPage = service.search(brand, page, size);
        return BaseResult.ok(brandPage);
    }
}
