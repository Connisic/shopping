package com.owner.shopping_seckill_service.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLock {
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 尝试对reids的键加锁
     * @param key redis的键
     * @param expireTime 键过期时间
     * @return 返回加锁是否成功
     */
    public boolean lock(String key, long expireTime){
        //生成对应键的锁对象
        RLock lock = redissonClient.getLock("lock:"+key);
        //尝试加锁
        try {
            //尝试加锁，设置过期时间
            return lock.tryLock(expireTime, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            //该键被其他用户获得锁，中断该线程
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return false;
        }
    }

    //用于订单支付成功，释放占有的锁
    public void unlock(String key){
        RLock lock = redissonClient.getLock("lock:" + key);
        if (lock.isLocked()){//锁还是被锁住
            lock.unlock();//释放拥有的锁
        }
    }
}
