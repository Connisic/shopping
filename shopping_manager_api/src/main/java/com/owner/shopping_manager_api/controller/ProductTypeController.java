package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.ProductType;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.ProductTypeService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productType")
public class ProductTypeController {
    @DubboReference
    private ProductTypeService service;

    /**
     * 新增商品类型
     * @param productType 新增商品类型对象
     * @return 执行结果
     */
    @PostMapping("/add")
    public BaseResult add(@RequestBody ProductType productType){
        service.add(productType);
        return BaseResult.ok();
    }

    /**
     * 修改商品类型
     * @param productType 要修改的商品类型
     * @return 执行结果
     */
    @PutMapping("/update")
    public BaseResult update(@RequestBody ProductType productType){
        service.update(productType);
        return BaseResult.ok();
    }

    /**
     * 删除商品类型
     * @param id 商品类型id
     * @return 执行结果
     */
    @DeleteMapping("/delete")
    public BaseResult delete(Long id){
        service.delete(id);
        return BaseResult.ok();
    }

    /**
     * 根据id查询商品类型
     * @param id
     * @return 查询结果
     */
    @GetMapping("/findById")
    public BaseResult<ProductType> findById(Long id){
        ProductType pr = service.findById(id);
        return BaseResult.ok(pr);
    }

    /**
     * 分页查询商品类型
     * @param productType 查询条件
     * @param page 页码
     * @param size 每页条数
     * @return 查询结果
     */
    @GetMapping("/search")
    public BaseResult<Page<ProductType>> search(ProductType productType,int page,int size){
        Page<ProductType> productTypePage = service.search(productType, page, size);
        return BaseResult.ok(productTypePage);
    }

    /**
     * 条件查询商品类型
     * @param productType 查询条件
     * @return 查询结果
     */
    @GetMapping("/findProductType")
    public BaseResult<List<ProductType>> findProductType(ProductType productType){
        List<ProductType> list = service.findProductType(productType);
        return BaseResult.ok(list);
    }

    /**
     * 根据上级id查询所柚子类型
     * @param parentId 上级id
     * @return 查询结果
     */
    @GetMapping("/findByParentId")
    public  BaseResult<List<ProductType>> findByParentId(Long parentId){
        ProductType productType = new ProductType();
        productType.setParentId(parentId);
        List<ProductType> list = service.findProductType(productType);
        return BaseResult.ok(list);
    }
}
