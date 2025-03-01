package com.owner.shopping_common.service;

import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.pojo.Payment;

import java.util.Map;
public interface ZfbPayService {
    /**
     * 根据订单信息生成二维码字符串
     * @param orders 订单对象
     * @return 返回二维码字符串，前端对该字符串处理成二维码
     */
    String pcPay(Orders orders);

    /**
     * 验证签名方法
     * @param paramMap 支付宝相关参数
     */
    void checkSign(Map<String, Object> paramMap);

    /**
     * 将支付信息保存到数据库中
     * @param payment 支付记录对象
     */
    void addPayment(Payment payment);
}
