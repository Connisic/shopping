package com.owner.shopping_common.service;

import com.owner.shopping_common.result.BaseResult;

//短信服务
public interface MessageService {
    //发送短信方法

    /**
     * 发送短信
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 放回封装结果，成功code200，失败有很多奇奇怪怪的信息，干脆直接封装成BaseResult
     */
    BaseResult sendMessage(String phoneNumber,String code);
}
