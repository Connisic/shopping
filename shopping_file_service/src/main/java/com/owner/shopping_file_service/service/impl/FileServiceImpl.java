package com.owner.shopping_file_service.service.impl;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.FileService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;

@DubboService
public class FileServiceImpl implements FileService {
    //fdfs上传文件客户端
    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    //获取配置文件中的fdfs.fileUrl,以便于前端通过该路径去访问fdfs服务器文件
    @Value("${fdfs.fileUrl}")
    private String fileUrl;

    @Override
    public String uploadImage(byte[] bytes, String fileName) {

        if (bytes.length!=0){
            try {
                //1将字节数组转换成输入流
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                //2获取文件的后缀名
                String fileSuffix = fileName.substring(fileName.lastIndexOf('.') + 1);
                //3上传文件
                StorePath storePath = fastFileStorageClient.uploadFile(in, in.available(), fileSuffix, null);


                //4返回图片路径
                String imageUrl=fileUrl+'/'+storePath.getFullPath();
                return imageUrl;
            }catch (Exception e){
                e.printStackTrace();
                throw new BusExceptiion(CodeEnum.UPLOAD_FILE_ERROR);

            }
        }else{
            throw new BusExceptiion(CodeEnum.UPLOAD_FILE_ERROR);
        }
    }
    @Override
    public String uploadImageByFile(String fileName){
        try(InputStream in=new FileInputStream(new File(fileName))){
            byte[] bytes = in.readAllBytes();
            return uploadImage(bytes,fileName);

        }catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new BusExceptiion(CodeEnum.UPLOAD_FILE_ERROR);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusExceptiion(CodeEnum.UPLOAD_FILE_ERROR);
        }
    }
    @Override
    public void delete(String filePath) {
        fastFileStorageClient.deleteFile(filePath);
    }
}
