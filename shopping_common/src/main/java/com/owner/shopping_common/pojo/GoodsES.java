package com.owner.shopping_common.pojo;


import lombok.Data;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.awt.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 在ES中存储的商品实体类
 */
@Document(indexName = "goods_index",createIndex = false)
@Data
public class GoodsES implements Serializable {
    @Field
    private Long id; // 商品id
    @Field
    private String goodsName; // 商品名称
    @Field
    private String caption; // 副标题
    @Field
    private BigDecimal price; // 价格
    @Field
    private String headerPic; // 头图
    @Field
    private String brand; // 品牌名称
    @CompletionField
    private List<String> tags; // 关键字
    @Field
    private List<String> productType; // 类目名
    @Field
    private Map<String,List<String>> specification; // 规格,键为规格项,值为规格值
    @Field
    private Integer sales; // 销量
    @Field
    private Double rating; // 评分
    @Field
    private String createTime; // 创建时间

}
