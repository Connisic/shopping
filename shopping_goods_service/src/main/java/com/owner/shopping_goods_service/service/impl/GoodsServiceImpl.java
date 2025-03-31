package com.owner.shopping_goods_service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.owner.shopping_common.pojo.*;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.FileService;
import com.owner.shopping_common.service.GoodsService;
import com.owner.shopping_common.service.SearchService;
import com.owner.shopping_goods_service.mapper.GoodsImageMapper;
import com.owner.shopping_goods_service.mapper.GoodsMapper;
import com.owner.shopping_goods_service.mapper.SpecificationMapper;
import com.owner.shopping_goods_service.mapper.SpecificationOptionMapper;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@DubboService
@Transactional
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {
    @DubboReference
    private FileService fileService;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsImageMapper goodsImageMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private SpecificationMapper specificationMapper;

    @Autowired
    private SpecificationOptionMapper optionMapper;


    //同步商品数据主题
    private final String SYNC_GOODS_QUEUE="sync_goods_queue";
    //删除商品数据主题
    private final String DEL_GOODS_QUEUE="del_goods_queue";
    //同步购物车商品数据主题
    private final String SYNC_CART_QUEUE="sync_cart_queue";
    //删除购物车商品数据主题
    private final String DEL_CART_QUEUE="del_cart_queue";
    @Override
    public void add(Goods goods) {
        //1插入商品数据，为该goods添加id
        goodsMapper.insert(goods);
        //2插入图片数据
        Long id = goods.getId();//获取商品主键id
        List<GoodsImage> images = goods.getImages();//商品图片集合
        images.forEach(image->{
            image.setGoodsId(id);
            goodsImageMapper.insert(image);
        });
        //插入商品规格项数据

        //1 获取规格
        List<Specification> specifications = goods.getSpecifications();

        //2 获取规格项
        // 遍历规格，获取规格项所有数据
        List<SpecificationOption> list=new ArrayList<>();
        specifications.forEach(specification -> {
            list.addAll(specification.getSpecificationOptions());
        });
        //插入数据
        list.forEach(option->{
            goodsMapper.addGoodsSpecificationOption(id,option.getId());
        });
        //System.out.println(goodsMapper.selectById(id));
        ////获取该goods详情对象
        //GoodsDesc goodsDesc = goodsMapper.findDesc(id);
        ////插入到Es中
        //if (goods.getIsMarketable()){
        //    //searchService.SyncGoodsToES(goodsDesc);
        //    rocketMQTemplate.syncSend(SYNC_GOODS_QUEUE,goodsDesc);//向同步队列发送商品详情对象
        //}
    }

    @Override
    public void addAll(ArrayList<Goods> goodsList) {
        try{
            for (Goods goods : goodsList) {
                // 上传文件
                String fileName = goods.getHeaderPic();
                String newPic = fileService.uploadImageByFile(fileName);

                // 设置图片
                List<GoodsImage> list = new ArrayList<>();
                GoodsImage image = new GoodsImage();
                image.setImageUrl(newPic);
                image.setImageTitle(goods.getGoodsName());
                list.add(image);

                // 设置商品属性
                goods.setImages(list);
                goods.setHeaderPic(newPic);
                goods.setSpecifications(new ArrayList<Specification>());

                // 保存商品
                add(goods);
            }
        }catch (DataIntegrityViolationException e){
            throw new BusExceptiion(CodeEnum.DATA_VIOLATION_ERROR);
        }
    }

    @Override
    public void update(Goods goods) {
        //修改商品
        //1 直接向数据库goods表修改商品
        goodsMapper.updateById(goods);

        //2 修改商品图片表，先删除与该商品相关的图片，再插入要修改的图片
        QueryWrapper<GoodsImage> wrapper = new QueryWrapper<>();
        wrapper.eq("goodsId",goods.getId());
        goodsImageMapper.delete(wrapper);
        //3  ，删除相关商品项，添加要修改的商品项
        goodsMapper.deleteGoodsSpecificationOption(goods.getId());

        //2插入图片数据
        Long id = goods.getId();//获取商品主键id
        List<GoodsImage> images = goods.getImages();//商品图片集合
        images.forEach(image->{
            image.setGoodsId(id);
            goodsImageMapper.insert(image);
        });
        //插入商品规格项数据

        //1 获取规格
        List<Specification> specifications = goods.getSpecifications();
        System.out.println(specifications);

        //2 获取规格项
        // 遍历规格，获取规格项所有数据
        List<SpecificationOption> list=new ArrayList<>();
        specifications.forEach(specification -> {
            if (specification.getId()==null)
                specificationMapper.insert(specification);
            //System.out.println(specification.getId()+"----------------------------------------");
            //list.addAll(specification.getSpecificationOptions());
            List<SpecificationOption> options = specification.getSpecificationOptions();
            options.forEach(option->{
                option.setSpecId(specification.getId());
                list.add(option);
            });
        });
        //插入数据
        list.forEach(option->{
            //System.out.println(option.getId()+"++++++++++++++++++++++++++++++++");
            if (option.getId()==null)
                optionMapper.insert(option);
            goodsMapper.addGoodsSpecificationOption(id,option.getId());
        });
        GoodsDesc goodsDesc = goodsMapper.findDesc(id);
        //searchService.delete(id);
        //if (goods.getIsMarketable()){
        //    searchService.SyncGoodsToES(goodsDesc);
        //}
        //向同步商品队列发送商品信息
        rocketMQTemplate.syncSend(SYNC_GOODS_QUEUE,goodsDesc);

        //向同步购物车商品队列发送商品信息，将更新后的商品信息从数据库同步到redis
        CartGoods cartGoods = new CartGoods();
        cartGoods.setGoodId(goods.getId());
        cartGoods.setGoodsName(goods.getGoodsName());
        cartGoods.setPrice(goods.getPrice());
        cartGoods.setHeaderPic(goods.getHeaderPic());
        rocketMQTemplate.syncSend(SYNC_CART_QUEUE,cartGoods);
    }

    @Override
    public Goods findById(Long id) {
        //return goodsMapper.findById(id);
        return goodsMapper.selectById(id);
    }

    @Override
    public void putAway(Long id, Boolean isMarcketable) {
        if (isMarcketable){//true,商品上架，发送商品信息到商品同步队列,

            Goods goods = goodsMapper.selectById(id);
            GoodsDesc goodsDesc=new GoodsDesc();
            goodsDesc.setGoodsName(goods.getGoodsName());
            goodsDesc.setHeaderPic(goods.getHeaderPic());

            rocketMQTemplate.syncSend(SYNC_GOODS_QUEUE,goodsDesc);


        }else{ //false,商品下架，发送到删除队列
            rocketMQTemplate.syncSend(DEL_GOODS_QUEUE,id);
            //发送商品id到购物车删除队列
            rocketMQTemplate.syncSend(DEL_CART_QUEUE,id);
        }
        goodsMapper.putAway(id,isMarcketable);
    }

    @Override
    public Page<Goods> search(Goods goods, int page, int size) {
        QueryWrapper<Goods> wrapper = new QueryWrapper<>();
        if(goods!=null&& StringUtils.hasText(goods.getGoodsName()))
            wrapper.like("goodsName",goods.getGoodsName());
        Page<Goods> goodsPage = goodsMapper.selectPage(new Page<>(page, size), wrapper);
        return goodsPage;
    }
    //查询所有商品详情
    @Override
    public List<GoodsDesc> findAllDesc() {
        List<GoodsDesc> all = goodsMapper.findAll();
        return all;
    }

    //查询所有商品
    @Override
    public List<Goods> findAll() {
        List<Goods> goods = this.list();
        return goods;
    }
    @Override
    public GoodsDesc findDesc(Long id) {
        return goodsMapper.findDesc(id);
    }

    @Override
    public void delete(Long id) {
        goodsMapper.deleteById(id);
    }


}
