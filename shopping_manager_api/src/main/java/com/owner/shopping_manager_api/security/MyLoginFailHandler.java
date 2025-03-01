package com.owner.shopping_manager_api.security;

import com.alibaba.fastjson2.JSON;
import com.owner.shopping_common.result.BaseResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

public class MyLoginFailHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        BaseResult baseResult=new BaseResult(402,"用户名或密码错误！",null);
        response.setContentType("text/json;charset=utf-8");
        response.getWriter().write(JSON.toJSONString(baseResult));
    }
}
