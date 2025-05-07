package com.owner.shopping_common.pojo;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName bz_user_goods_score
 */
@Data
@NoArgsConstructor
public class UserGoodsScore implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userid;

    private Long goodsid;

    private String comment;

    private Double score;

    private Date createTime;

    private Date updateTime;
    public UserGoodsScore(Long userid, Long goodsid, String comment, Double score) {
    	this.userid = userid;
    	this.goodsid = goodsid;
    	this.comment = comment;
    	this.score = score;
        createTime=new Date();
        updateTime=new Date();
    }
}