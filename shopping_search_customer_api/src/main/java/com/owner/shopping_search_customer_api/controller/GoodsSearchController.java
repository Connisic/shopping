package com.owner.shopping_search_customer_api.controller;

import com.owner.shopping_common.pojo.GoodsDesc;
import com.owner.shopping_common.pojo.GoodsSearchParam;
import com.owner.shopping_common.pojo.GoodsSearchResult;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.GoodsService;
import com.owner.shopping_common.service.SearchService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/goodsSearch")
public class GoodsSearchController {
    @DubboReference
    private SearchService searchService;

    @DubboReference
    private GoodsService goodsService;
    //自动补齐关键字
    @GetMapping("/autoSuggest")
    BaseResult<List<String>> autoSuggest(String keyword){
        List<String> keywords = searchService.autoSuggest(keyword);
        return BaseResult.ok(keywords);
    }

    @PostMapping("/search")
    BaseResult<GoodsSearchResult> search(@RequestBody GoodsSearchParam goodsSearchParam){
        GoodsSearchResult result = searchService.search(goodsSearchParam);
        return BaseResult.ok(result);
    }

    @GetMapping("/findDesc")
    BaseResult<GoodsDesc> findDesc(Long id){
        GoodsDesc goodsDesc = goodsService.findDesc(id);
        return BaseResult.ok(goodsDesc);
    }
}
