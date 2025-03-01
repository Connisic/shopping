package com.owner.shopping_file_service;

import com.github.tobato.fastdfs.FdfsClientConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDubbo
@EnableDiscoveryClient
@RefreshScope
@Import(FdfsClientConfig.class)//手动导入fdfs配置文件
public class ShoppingFileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingFileServiceApplication.class, args);
    }

}
