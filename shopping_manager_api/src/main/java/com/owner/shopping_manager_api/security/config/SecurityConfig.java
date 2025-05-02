package com.owner.shopping_manager_api.security.config;

import com.owner.shopping_manager_api.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

//security配置类
@Configuration
@EnableWebSecurity
//开启鉴权配置注解
@EnableMethodSecurity
public class SecurityConfig {
    //配置security配置
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http)throws Exception{
        //认证，配置表单登录
        http.formLogin(
          form -> {
              form.usernameParameter("username")//用户名
                      .passwordParameter("password")//密码
                      .loginProcessingUrl("/admin/login")//登陆路径
                      .successHandler(new MyLoginSuccessHandler())//登陆成功处理器
                      .failureHandler(new MyLoginFailHandler());//登陆失败处理器
          }
        );
        //配置权限
        http.authorizeHttpRequests(
                resp -> {
                    resp.requestMatchers("/login","/admin/login").permitAll();//登录放行
                    resp.anyRequest().authenticated();//其余请求都需要认证
                }
        );
        //登出配置
        http.logout(
                logout -> {
                    logout.logoutUrl("/admin/logout")
                            .logoutSuccessHandler(new MyLogoutSuccessHandler())//登出成功处理器
                            .clearAuthentication(true)//清空认证
                            .invalidateHttpSession(true);//删除session
                }
        );
        http.exceptionHandling(
                exception ->{
                    exception.authenticationEntryPoint(new MyAuthenticationEntryPoint())//未登录处理器
                    .accessDeniedHandler(new MyAccessDeniedHandler());//权限不足处理器
                }
        );
        //跨域访问
        http.cors();
        http.csrf(csrf->csrf.disable());
        return http.build();
    }

    //加密工具
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
