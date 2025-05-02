package com.owner.shopping_file_service;


import com.owner.shopping_common.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShoppingFileServiceApplicationTests {
    @Autowired
    private FileService fileService;
    @Test
    void contextLoads() {
        System.out.println(fileService.uploadImageByFile("B:\\桌面\\shopAc\\images\\30.jpg"));
    }

}
