package com.owner.shopping_pay_service.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.pojo.Payment;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.ZfbPayService;
import com.owner.shopping_pay_service.ZfbPayConfig;
import com.owner.shopping_pay_service.mapper.PaymentMapper;
import com.owner.shopping_pay_service.util.ZfbVerifierUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@DubboService
public class ZfbPayServiceImpl implements ZfbPayService {
    @Autowired
    private ZfbPayConfig zfbPayConfig;
    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private PaymentMapper paymentMapper;
    @Override
    public String pcPay(Orders orders) {
        /**
         * 判断订单状态是否支付
         */
        if(orders.getStatus()!=1){
            throw new BusExceptiion(CodeEnum.ORDERS_STATUS_ERROR);
        }
        // 1.创建请求对象
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        // 2.设置请求内容
        request.setNotifyUrl(zfbPayConfig.getNotifyUrl() + zfbPayConfig.getPcNotify());
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orders.getId()); // 订单编号
        bizContent.put("total_amount", orders.getPayment()); // 订单金额
        bizContent.put("subject", orders.getCartGoods().get(0).getGoodsName()); //订单标题
        request.setBizContent(bizContent.toString());

        try {
            // 3.发送请求
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            // 4.返回二维码
            return response.getQrCode();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new BusExceptiion(CodeEnum.QR_CODE_ERROR);
        }
    }

    @Override
    public void checkSign(Map<String, Object> paramMap) {
        //获取所有参数
        Map<String, String[]> requestParameterMap = (Map<String, String[]>) paramMap.get("requestParameterMap");
        //验签
        boolean valid = ZfbVerifierUtils.isValid(requestParameterMap, zfbPayConfig.getPublicKey());
        //失败抛出异常
        if (!valid){
            throw new BusExceptiion(CodeEnum.CHECK_SIGN_ERROR);
        }
    }

    @Override
    public void addPayment(Payment payment) {
        paymentMapper.insert(payment);
    }
}
