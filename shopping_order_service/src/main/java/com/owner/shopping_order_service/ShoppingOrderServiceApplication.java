package com.owner.shopping_order_service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
@RefreshScope
@MapperScan("com.owner.shopping_order_service.mapper")
public class ShoppingOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingOrderServiceApplication.class, args);
    }

}
