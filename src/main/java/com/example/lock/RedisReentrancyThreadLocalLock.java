package com.example.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 ThreadLocal 内存可重入分布式锁<br>
 * 分布式锁详情参考：<br>
 * SimpleRedisLock
 *
 * @author andyXu xu9529@gmail.com
 * @date 2020/6/13
 */
@Service
public class RedisReentrancyThreadLocalLock {

    private static ThreadLocal<Map<String, Integer>> LOCKS = ThreadLocal.withInitial(HashMap::new);

    @Autowired
    SimpleRedisLock redisLock;

    /**
     * 可重入锁
     *
     * @param lockName  锁名字,代表需要争临界资源
     * @param request   唯一标识，可以使用 uuid，根据该值判断是否可以重入
     * @param leaseTime 锁释放时间
     * @param unit      锁释放时间单位
     * @return
     */
    public Boolean tryLock(String lockName, String request, long leaseTime, TimeUnit unit) {
        Map<String, Integer> counts = LOCKS.get();
        if (counts.containsKey(lockName)) {
            counts.put(lockName, counts.get(lockName) + 1);
            return true;
        } else {
            if (redisLock.tryLock(lockName, request, leaseTime, unit)) {
                counts.put(lockName, 1);
                return true;
            }
        }
        return false;
    }

    /**
     * 解锁需要判断不同线程池
     *
     * @param lockName
     * @param request
     */
    public void unlock(String lockName, String request) {
        Map<String, Integer> counts = LOCKS.get();
        if (counts.getOrDefault(lockName, 0) <= 1) {
            counts.remove(lockName);
            Boolean result = redisLock.unlock(lockName, request);
            if (!result) {
                throw new IllegalMonitorStateException("attempt to unlock lock, not locked by lockName:+" + lockName + " with request: "
                        + request);
            }

        } else {
            counts.put(lockName, counts.get(lockName) - 1);
        }
    }


    /**
     * 查看是否加锁
     *
     * @param lockName
     * @return
     */
    public Boolean isLocked(String lockName) {
        Map<String, Integer> counts = LOCKS.get();
        return (counts.getOrDefault(lockName, 0) >= 1);
    }


    /**
     * 强制解锁
     *
     * @param lockName
     * @return true:解锁成功，false：锁不存在，或者锁已经超时，
     */
    public Boolean forceUnlock(String lockName) {
        Map<String, Integer> counts = LOCKS.get();
        counts.remove(lockName);
        return redisLock.forceUnlock(lockName);
    }


}
