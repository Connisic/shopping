package com.owner.shopping_goods_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.GoodsDesc;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsMapper extends BaseMapper<Goods>  {
    //添加商品规格项
    void addGoodsSpecificationOption(@Param("gid") Long gid,@Param("optionId") Long optionId);
    //删除商品规格项所有规格项
    void deleteGoodsSpecificationOption(Long gid);

    //商品的上架或下架
    void putAway(@Param("id")Long id,@Param("isMarketable") Boolean isMarketable);

    Goods findById(Long gid);


    //查询所有商品详情
    List<GoodsDesc> findAll();

    //根据id查询商品详情
    GoodsDesc findDesc(Long id);
}
