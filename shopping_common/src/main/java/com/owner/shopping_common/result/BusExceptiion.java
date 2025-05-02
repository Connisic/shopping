package com.owner.shopping_common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 自定义业务异常类
 */
@Data
@AllArgsConstructor
//@NoArgsConstructor
public class BusExceptiion extends RuntimeException implements Serializable {

    //状态码
    private Integer code;
    //异常消息
    private String message;

    public BusExceptiion(CodeEnum codeEnum){
        this.code=codeEnum.getCode();
        this.message= codeEnum.getMessage();
    }
}
