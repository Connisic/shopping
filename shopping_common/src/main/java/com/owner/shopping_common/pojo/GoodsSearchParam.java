package com.owner.shopping_common.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品搜索条件
 */
@Data
public class GoodsSearchParam implements Serializable {

    private String keyword;

    private String brand;

    private Double highPrice;
    

    private Double lowPrice;


    private Map<String, String> specificationOption;

    private String sortFiled;

    private String sort;

    private Integer page;

    private Integer size;

}