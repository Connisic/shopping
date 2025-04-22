package com.owner.shopping_common.component;

import com.owner.shopping_common.pojo.Goods;
import com.owner.shopping_common.pojo.GoodsDesc;
import org.springframework.cglib.core.internal.Function;

public class Goods2DescFunction implements Function<Goods,GoodsDesc > {
    public static final Goods2DescFunction INSTANCE = new Goods2DescFunction();
    public Goods2DescFunction one(){
        return INSTANCE;
    }

    @Override
    public GoodsDesc apply(Goods key) {
        GoodsDesc goodsDesc = new GoodsDesc();
        goodsDesc.setGoodsName(key.getGoodsName());
        goodsDesc.setHeaderPic(key.getHeaderPic());
        goodsDesc.setId(key.getId());
        goodsDesc.setIsMarketable(key.getIsMarketable());
        goodsDesc.setIntroduction(key.getIntroduction());
        goodsDesc.setCaption(key.getCaption());
        goodsDesc.setPrice(key.getPrice());
        goodsDesc.setSales(key.getSales());
        goodsDesc.setRating(key.getRating());
        goodsDesc.setCreateTime(key.getCreateTime());
        return goodsDesc;
    }
}
