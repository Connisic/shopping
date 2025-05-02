package com.owner.shopping_recommend_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.UserGoodsScore;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.RecommendService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/recommend")
public class RecommendController {
	@DubboReference
	private RecommendService service;

	//分页查询推荐商品
	@GetMapping("/searchPage")
	public BaseResult<Page> searchByPage(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestHeader(value = "userId", required = false) Long userId) {
		// 判断用户是否登录（userId 为空表示未登录）
		Page<Goods> goodsPage;
		if (userId == null) {
			// 未登录用户推荐逻辑：获取热门商品或默认推荐
			goodsPage = service.searchRecGoods(page, size, 28L);
		} else {
			// 已登录用户推荐逻辑：基于用户ID的个性化推荐
			goodsPage = service.searchRecGoods(page, size, userId);
		}

		return BaseResult.ok(goodsPage);
	}

	@PostMapping("/addComment")
	public BaseResult<Void> addComment(@RequestHeader Long userId,
	                                   @RequestParam Long goodsId,
	                                   @RequestParam String comment,
	                                   @RequestParam Double score) {
		service.addUserComment(userId, goodsId, comment, score);
		return BaseResult.ok();
	}

	@PutMapping("/updateComment")
	public BaseResult<Void> updateComment(@RequestHeader Long userId,
	                                      @RequestParam Long goodsId,
	                                      @RequestParam String comment,
	                                      @RequestParam Double score) {
		service.updateUserComment(userId, goodsId, comment, score);
		return BaseResult.ok();
	}

	@DeleteMapping("/deleteComment")
	public BaseResult<Void> deleteComment(@RequestHeader Long userId,
	                                      @RequestParam Long goodsId) {
		service.deleteUserComment(userId, goodsId);
		return BaseResult.ok();
	}

	@GetMapping("/getComment")
	public BaseResult<Page<UserGoodsScore>> getComment(@RequestParam(defaultValue = "0") int page,
	                                                   @RequestParam(defaultValue = "10") int size) {
		return BaseResult.ok(service.getUserComment(page,size));
	}
}
