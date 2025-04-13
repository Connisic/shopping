package com.owner.shopping_common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 状态码枚举类
 */
@Getter
@AllArgsConstructor
public enum CodeEnum {
    //正常
    SUCCESS(200,"OK!"),
    //错误
    SYSTEM_ERROR(500,"系统异常!"),

    PARAMETER_ERROR(601,"参数异常!"),

    //添加商品类型异常
    INSERT_PRODUCT_TYPE_ERROR(602,"三级商品类型不能添加子类型"),

    //类型删除失败
    DELETE_PRODUCT_TYPE_ERROR(603,"该类型有子类型不能删除！"),

    UPLOAD_FILE_ERROR(604,"文件上传异常"),

    REGISTER_CODE_ERROR(605,"注册验证码错误"),


    REPEAT_PHONE_ERROR(606,"手机号重复"),

    REPEAT_USERNAME_ERROR(607,"用户名重复"),

    USER_LOGIN_ERROR(608,"用户名或密码错误"),

    LOGIN_CODE_ERROR(609,"登录验证码错误"),

    LOGIN_NOPHONE_ERROR(610,"手机号未注册"),

    LOGIN_STATUS_ERROR(611,"用户状态异常"),


    QR_CODE_ERROR(612,"获取支付二维码失败"),

    CHECK_SIGN_ERROR(613,"支付宝验签异常"),

    ORDERS_STATUS_ERROR(614,"订单支付异常"),

    ORDERS_EXPIRED_ERROR(615,"订单不存在或订单异常"),

    NO_STOCK_ERROR(616,"库存不足"),

    NO_USER_GOODS_ERROR(617,"不存在用户名/商品名"),

    DATA_VIOLATION_ERROR(618,"数据访问异常"),

    UPDATE_MATRIX_ERROR(619,"更新相似度矩阵失败" ),

    BATCH_QUERY_ERROR(620, "批量获取商品年龄数据失败");
    private final  Integer code;

    private final String message;

}
