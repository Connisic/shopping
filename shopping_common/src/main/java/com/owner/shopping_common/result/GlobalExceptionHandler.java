package com.owner.shopping_common.result;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    //处理业务异常
    @ExceptionHandler(BusExceptiion.class)
    public BaseResult defaultExceptionHandler(BusExceptiion e){
        e.printStackTrace();
        BaseResult baseResult = new BaseResult(e.getCode(), e.getMessage(), null);
        return baseResult;
    }


    //处理系统异常
    @ExceptionHandler(Exception.class)
    public BaseResult defaultExceptionHandler(Exception e){
        e.printStackTrace();
        BaseResult baseResult = new BaseResult(CodeEnum.SYSTEM_ERROR.getCode(), CodeEnum.SYSTEM_ERROR.getMessage(), null);
        return baseResult;
    }
}
