package com.owner.shopping_goods_service;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDubbo //开启dubbo
@EnableDiscoveryClient  //向注册中心注册服务
@RefreshScope  //动态刷新


@MapperScan("com.owner.shopping_goods_service.mapper")
public class ShoppingGoodsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingGoodsServiceApplication.class, args);
    }
    //配置分页插件  没有配置分页插件，分页查询会直接返回所有结果，而不是按照分页的方式返回数据
    //没有分页插件，系统会直接加载整个数据集，尤其在数据量较大的情况下，这会显著影响系统性能
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
