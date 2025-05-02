package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.SeckillGoods;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.SecKillGoodsService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckillGoods")
public class SecKillGoodsController {
    @DubboReference
    private SecKillGoodsService service;

    /**
     * 新增秒杀商品
     * @param seckillGoods 秒杀商品
     * @return 执行结果
     */
    @PostMapping("/add")
    public BaseResult add(@RequestBody SeckillGoods seckillGoods){
        service.add(seckillGoods);
        return BaseResult.ok();
    }

    /**
     * 修改秒杀商品
     * @param seckillGoods 要修改的秒杀商品
     * @return 执行结果
     */
    @PutMapping("/update")
    public BaseResult update(@RequestBody SeckillGoods seckillGoods){
        service.update(seckillGoods);
        return BaseResult.ok();
    }

    /**
     * 分页查询秒杀商品
     * @param page 页码
     * @param size 每页条数
     * @return 查询结果
     */
    @GetMapping("/findPage")
    public BaseResult<Page<SeckillGoods>> search(int page,int size){
        Page<SeckillGoods> search = service.search(page, size);
        return BaseResult.ok(search);
    }
}
