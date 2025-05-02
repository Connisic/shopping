package com.owner.shopping_search_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ES同步监控类
 */
@Slf4j
@Component
public class SyncMonitor {

    // 同步成功计数器
    private AtomicInteger successCounter = new AtomicInteger(0);
    
    // 同步失败计数器
    private AtomicInteger failCounter = new AtomicInteger(0);
    
    // 运行中计数器
    private AtomicInteger runningCounter = new AtomicInteger(0);
    
    // 告警阈值，失败数超过这个值时触发告警
    @Value("${es.sync.alert.threshold:100}")
    private int alertThreshold;

    /**
     * 记录同步成功
     */
    public void recordSuccess() {
        successCounter.incrementAndGet();
        decreaseRunning();
    }

    /**
     * 记录同步失败
     */
    public void recordFailure() {
        failCounter.incrementAndGet();
        decreaseRunning();
    }

    /**
     * 增加运行中任务数
     */
    public void increaseRunning() {
        runningCounter.incrementAndGet();
    }

    /**
     * 减少运行中任务数
     */
    private void decreaseRunning() {
        runningCounter.decrementAndGet();
    }

    /**
     * 重置计数器
     */
    public void reset() {
        successCounter.set(0);
        failCounter.set(0);
        runningCounter.set(0);
    }

    /**
     * 检查同步任务状态
     * @param failedIds 失败的商品ID集合
     */
    public void checkStatus(Set<Long> failedIds) {
        int failedCount = failedIds.size();
        
        if (failedCount > alertThreshold) {
            log.error("ES同步告警：失败商品数量过多，数量: {}, 超过阈值: {}", failedCount, alertThreshold);
            // 这里可以添加发送邮件或短信告警的逻辑
        }
        
        log.info("同步状态 - 成功: {}, 失败: {}, 运行中: {}, 失败商品数: {}", 
                successCounter.get(), failCounter.get(), runningCounter.get(), failedCount);
    }

    /**
     * 每小时输出一次同步状态报告
     */
    @Scheduled(fixedRate = 3600000)
    public void reportStatus() {
        log.info("=== ES同步状态报告 ===");
        log.info("累计成功同步: {}", successCounter.get());
        log.info("累计失败同步: {}", failCounter.get());
        log.info("当前运行中任务: {}", runningCounter.get());
        
        // 每天零点重置计数器
        if (java.time.LocalTime.now().getHour() == 0) {
            reset();
            log.info("计数器已重置");
        }
    }
} 