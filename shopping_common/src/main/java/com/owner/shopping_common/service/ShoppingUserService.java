package com.owner.shopping_common.service;

import com.owner.shopping_common.pojo.ShoppingUser;

import java.util.List;

//商城用户服务接口
public interface ShoppingUserService {
    //注册时向redis中保存发送的手机号+验证码
    void saveRegisterCheckCode(String phoneNum,String code);
    //注册是验证手机号和验证码
    void registerCheckCode(String phoneNum,String code);
    //用户注册
    void register(ShoppingUser user);
    //用户名密码登录
    String loginPassword(String username,String password);

    //登陆时redis保存手机号+验证码
    void saveLoginCheckCode(String phoneNum,String code);

    //手机验证码登录
    String loginCheckCode(String phoneNum,String code);
    //获取登录用户名
    String getName(String token);
    //根据id查询用户
    ShoppingUser findById(String token);
    //查询所有用户
    List<ShoppingUser> getAllUser();
    void checkPhone(String phone);
}