package com.owner.shopping_common.pojo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName bz_user_goods_score
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserGoodsScore implements Serializable {
    private Long userid;

    private Long goodsid;

    private String comment;

    private Double score;

    private static final long serialVersionUID = 1L;
}