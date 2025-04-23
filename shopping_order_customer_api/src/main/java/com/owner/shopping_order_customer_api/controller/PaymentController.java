package com.owner.shopping_order_customer_api.controller;

import com.alibaba.fastjson2.JSON;
import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.pojo.Payment;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.OrderService;
import com.owner.shopping_common.service.ZfbPayService;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//支付
@RestController
@RequestMapping("/user/order")
public class PaymentController {
    @DubboReference
    private ZfbPayService zfbPayService;
    
    @DubboReference
    private OrderService orderService;

    /**
     * 生成支付宝支付二维码
     * @param orderId 订单id
     * @return 返回二维码字符串
     */
    @PostMapping("/payment/pcPay")
    public BaseResult<String> pcPay(String orderId){
        Orders orders = orderService.findById(orderId);
        String codeUrl= zfbPayService.pcPay(orders);

        return BaseResult.ok(codeUrl);
    }

    @PostMapping("/payment/success/notify")
    @GlobalTransactional
    public BaseResult successNotify(HttpServletRequest request){
        //验签
        Map<String, Object> paramMap = new HashMap<>();
        //验签序列化失败异常：由于request.getParameterMap()得到的map是一个只读map，
        // 对他修改以及序列化可能失败
        Map<String, String[]> newMap = request.getParameterMap();
        //新建一个普通hashMap去保存参数信息
        Map<String,String[]> map=new HashMap<>();
        for (String s : newMap.keySet()) {
            map.put(s,newMap.get(s));
        }
        paramMap.put("requestParameterMap",map);
        zfbPayService.checkSign(paramMap);

        //修改订单状态
        String trade_status = request.getParameter("trade_status"); //交易状态
        String out_trade_no = request.getParameter("out_trade_no"); //订单编号
        //如果支付成功
        if(trade_status.equals("TRADE_SUCCESS")){
            //修改订单状态
            Orders orders = orderService.findById(out_trade_no);
            orders.setStatus(2);//2：已支付
            orders.setPaymentType(2);//2：支付宝支付
            orders.setPaymentTime(new Date());
            orderService.update(orders);

            int i= 1/0;
            //添加交易记录
            Payment payment = new Payment();
            payment.setCreateTime(new Date());//交易时间
            payment.setOrderId(out_trade_no);//订单id
            payment.setTransactionId(out_trade_no); //交易编号
            payment.setPayerTotal(orders.getPayment()); //交易金额
            payment.setTradeState(trade_status); //交易状态
            payment.setTradeType("支付宝支付"); //交易方式
            payment.setContent(JSON.toJSONString(request.getParameterMap())); //交易详情

            zfbPayService.addPayment(payment);
        }
        return BaseResult.ok();
    }

}
