package com.owner.shopping_file_service.listener;

import com.owner.shopping_common.service.FileService;
import com.owner.shopping_file_service.MyFile;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;

@RocketMQMessageListener(topic = "sync_file_topic",consumerGroup = "sync_file_group")
public class SyncFileListener implements RocketMQListener<MyFile> {
    @Autowired
    private FileService fileService;
    @Override
    public void onMessage(MyFile myFile) {
        fileService.uploadImage(myFile.getFile(), myFile.getFileName());
    }
}
