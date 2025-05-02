package com.owner.shopping_common.result;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;


@Data
@AllArgsConstructor
public class BaseResult <T> implements Serializable {
    //状态码 200成功，失败其他
    private Integer code;
    //提示消息
    private String message;
    //数据
    private T data;

    //构建成功结果
    public static <T> BaseResult<T> ok(){
        return new BaseResult<>(CodeEnum.SUCCESS.getCode(), CodeEnum.SUCCESS.getMessage(), null);
    }
    //构建带有数据的成功结果
    public static <T> BaseResult<T> ok(T data){
        return new BaseResult<>(CodeEnum.SUCCESS.getCode(), CodeEnum.SUCCESS.getMessage(), data);
    }
}
