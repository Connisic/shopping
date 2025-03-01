package com.owner.shopping_manager_api.controller;

import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.FileService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/file")
public class FileController {
    @DubboReference
    private FileService fileService;

    /**
     * 上传文件
     * @param file 文件
     * @return 返回文件网络路径
     * @throws IOException
     */
    @PostMapping("/uploadImage")
    public BaseResult<String> upload( MultipartFile file) throws IOException {
        //MultipartFile文件不能在服务间传递，没有实现Serializable接口
        byte[] bytes = file.getBytes();
        String url = fileService.uploadImage(bytes, file.getOriginalFilename());

        return BaseResult.ok(url);
    }

    @DeleteMapping("/delete")
    public BaseResult delete(String filePath){
        fileService.delete(filePath);

        return BaseResult.ok();
    }
}
