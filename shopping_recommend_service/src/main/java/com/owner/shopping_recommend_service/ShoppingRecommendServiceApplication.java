package com.owner.shopping_recommend_service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
@RefreshScope
@EnableScheduling
@MapperScan("com.owner.shopping_recommend_service.mapper")
public class ShoppingRecommendServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShoppingRecommendServiceApplication.class, args);
    }

}
