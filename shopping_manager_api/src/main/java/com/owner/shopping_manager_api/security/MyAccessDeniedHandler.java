package com.owner.shopping_manager_api.security;

import com.alibaba.fastjson2.JSON;
import com.owner.shopping_common.result.BaseResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
//权限处理器

/**
 * 注意：权限不足处理器的优先级低于全局异常处理器，
 * 所以当发生权限不足的时候该处理器不生效，
 * 只有全局再次把异常抛出才可以捕获
 */
public class MyAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        BaseResult baseResult=new BaseResult(403,"权限不足！",null);
        response.setContentType("text/json;charset=utf-8");
        response.getWriter().write(JSON.toJSONString(baseResult));
    }
}
