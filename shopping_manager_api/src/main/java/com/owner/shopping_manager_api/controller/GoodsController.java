package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.GoodsImage;
import com.owner.shopping_common.pojo.Specification;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.FileService;
import com.owner.shopping_common.service.GoodsService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/goods")
public class GoodsController {
    @DubboReference
    private GoodsService goodsService;

    /**
     * 新增商品
     * @param goods 商品对象
     * @return 执行结果
     */
    @PostMapping("/add")
    public BaseResult add(@RequestBody Goods goods){
        goodsService.add(goods);
        return BaseResult.ok();
    }
    @PostMapping("/addAll")
    public BaseResult addAll(@RequestBody ArrayList< Goods> goodsList){
        goodsService.addAll(goodsList);
        return BaseResult.ok();
    }

    /**
     * 修改商品
     * @param goods 商品对象
      * @return 执行结果
     */
    @PutMapping("/update")
    public BaseResult update(@RequestBody Goods goods){
        goodsService.update(goods);
        return BaseResult.ok();
    }

    /**
     * 商品上下架
     * @param id 商品id
     * @param isMarketable 是否上架或下架
     * @return 执行结果
     */
    @PutMapping("/putAway")
    public BaseResult putAway(Long id,Boolean isMarketable){
        goodsService.putAway(id,isMarketable);
        return BaseResult.ok();
    }

    /**
     * 根据id查询商品
     * @param id 商品id
     * @return 返回商品数据
     */
    @GetMapping("/findById")
    public BaseResult<Goods> findById(Long id){
        Goods goods = goodsService.findById(id);
        return BaseResult.ok(goods);
    }

    @DeleteMapping("/delete")
    public BaseResult deleteById(Long id){
        goodsService.delete(id);
        return BaseResult.ok();
    }
    /**
     * 分页查询商品
     * @param goods 分页查询条件
     * @param page 页码
     * @param size 每页条数
     * @return 查询结果
     */
    @GetMapping("/search")
    public BaseResult<Page<Goods>> findById(Goods goods,int page,int size ){
        Page<Goods> goodsPage = goodsService.search(goods, page, size);
        return BaseResult.ok(goodsPage);
    }
}
