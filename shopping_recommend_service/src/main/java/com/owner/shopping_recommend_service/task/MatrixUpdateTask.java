package com.owner.shopping_recommend_service.task;

import com.owner.shopping_common.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatrixUpdateTask {
    
    @Autowired
    private RecommendService recommendService;
    
    /**
     * 每天凌晨2点更新用户-物品评分矩阵
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void updateUserItemMatrix() {
        recommendService.generateMatrix();
    }
    
    /**
     * 每小时更新一次相似度矩阵
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void updateSimilarityMatrix() {
        recommendService.updateSimilarityMatrix();
    }
} 