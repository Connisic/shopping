package com.owner.shopping_seckill_service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDubbo
@EnableDiscoveryClient
@RefreshScope
@MapperScan("com.owner.shopping_seckill_service.mapper")
@SpringBootApplication
@EnableScheduling
public class ShoppingSeckillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingSeckillServiceApplication.class, args);
    }

    //// 布隆过滤器
    //@Bean
    //public BitMapBloomFilter bloomFilter(){
    //    // 构造方法的参数 决定了布隆过滤器能存放多少元素
    //    BitMapBloomFilter filter = new BitMapBloomFilter(10000);
    //    return filter;
    //}

}
