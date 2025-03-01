package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Specification;
import com.owner.shopping_common.pojo.SpecificationOptions;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.SpecificationService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/specification")
public class SpecificationController {
    @DubboReference
    private SpecificationService service;

    /**
     * 新增商品规格
     * @param specification 商品规格
     * @return 执行结果
     */
    @PostMapping("/add")
    public BaseResult add(@RequestBody Specification specification){
        service.add(specification);
        return BaseResult.ok();
    }

    /**
     * 修改商品规格
     * @param specification 要修改的商品规格
     * @return 执行结果
     */
    @PutMapping("/update")
    public BaseResult update(@RequestBody Specification specification){
        service.update(specification);
        return BaseResult.ok();
    }

    /**
     * 根据商品规格id批量删除商品规格
     * @param ids 商品规格id
     * @return 执行结果
     */
    @DeleteMapping("/delete")
    public BaseResult delete(Long [] ids){
        service.delete(ids);
        return BaseResult.ok();
    }

    /**
     * 根据id查询商品规格
     * @param id 商品规格id
     * @return 查询结果
     */
    @GetMapping("/findById")
    public BaseResult<Specification> findById(Long id){
        Specification s = service.findById(id);
        return BaseResult.ok(s);
    }

    /**
     * 分页查询商品规格
     * @param page 页码
     * @param size 每页条数
     * @return 查询结果
     */
    @GetMapping("/search")
    public BaseResult<Page<Specification>> search(int page,int size){
        Page<Specification> page1 = service.search(page, size);
        return BaseResult.ok(page1);
    }

    /**
     * 根据商品类型id查询商品项
     * @param id 商品类型id
     * @return 查询结果
     */
    @GetMapping("/findByProductTypeId")
    public BaseResult<List<Specification>> findByProductTypeId(Long id){
        List<Specification> spec = service.findByProductTypeId(id);
        return BaseResult.ok(spec);
    }

    /**
     * 批量新增商品项
     * @param specificationOptions 封装多个商品项
     * @return 执行结果
     */
    @PostMapping("/addOption")
    public BaseResult addOption(@RequestBody SpecificationOptions specificationOptions){
        service.addOption(specificationOptions);
        return BaseResult.ok();
    }

    /**
     * 批量删除商品项
     * @param ids 商品项id集合
     * @return 执行结果
     */
    @DeleteMapping("/deleteOption")
    public BaseResult deleteOption(Long[] ids){
        service.deleteOption(ids);
        return BaseResult.ok();
    }

}
