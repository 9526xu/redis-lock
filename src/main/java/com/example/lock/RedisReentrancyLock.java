package com.example.lock;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 如果使用 redis cluster 且底层数据连接采用 jedis ，需要使用 spring-data-redis 高于 2.1.9 的版本。
 * 暂不清楚底层数据连接采用 Lettuce 是否也存在版本兼容问题，使用前请先测试
 *
 * @author andyXu xu9529@gmail.com
 * @date 2020/6/13
 */
@Slf4j
@Service("redisReentrancyNewLock")
public class RedisReentrancyLock {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Boolean> lockScript;

    private DefaultRedisScript<Long> unlockScript;

    private DefaultRedisScript<Boolean> forceUnlockScript;

    @PostConstruct
    private void init() {
        try {

            String lockLuaScript = IOUtils.toString(ResourceUtils.getURL("classpath:lock.lua").openStream(), Charsets.UTF_8);
            lockScript = new DefaultRedisScript<>(lockLuaScript, Boolean.class);
            String unlockLuaScript = IOUtils.toString(ResourceUtils.getURL("classpath:unlock.lua").openStream(), Charsets.UTF_8);
            unlockScript = new DefaultRedisScript<>(unlockLuaScript, Long.class);
            String forceUnlockLuaScript = IOUtils.toString(ResourceUtils.getURL("classpath:force_unlock.lua").openStream(), Charsets.UTF_8);
            forceUnlockScript = new DefaultRedisScript<>(forceUnlockLuaScript, Boolean.class);

        } catch (IOException e) {
            log.error("RedisLockService init failed", e);
        }
    }

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
        long internalLockLeaseTime = unit.toMillis(leaseTime);
        return stringRedisTemplate.execute(lockScript, Lists.newArrayList(lockName), String.valueOf(internalLockLeaseTime), request);
    }

    /**
     * 解锁
     * 若可重入 key 次数大于 1，将可重入 key 次数减 1 <br>
     * 解锁 lua 脚本返回含义：<br>
     * 1:代表解锁成功 <br>
     * 0:代表锁未释放，可重入次数减 1 <br>
     * nil：代表其他线程尝试解锁 <br>
     * <p>
     * 如果使用 DefaultRedisScript<Boolean>，由于 Spring-data-redis eval 类型转化，<br>
     * 当 Redis 返回  Nil bulk, 默认将会转化为 false，将会影响解锁语义，所以下述使用：<br>
     * DefaultRedisScript<Long>
     * <p>
     * 具体转化代码请查看：<br>
     * JedisScriptReturnConverter<br>
     *
     * @param lockName 锁名称
     * @param request  唯一标识，可以使用 uuid
     * @throws IllegalMonitorStateException 解锁之前，请先加锁。若为加锁，解锁将会抛出该错误
     */
    public void unlock(String lockName, String request) {
        Long result = stringRedisTemplate.execute(unlockScript, Lists.newArrayList(lockName), request);
        // 如果未返回值，代表其他线程尝试解锁
        if (result == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by lockName:+" + lockName + " with request: "
                    + request);
        }
    }

    /**
     * 查看是否加锁
     *
     * @param lockName
     * @return
     */
    public Boolean isLocked(String lockName) {
        return stringRedisTemplate.hasKey(lockName);
    }

    /**
     * 强制解锁
     *
     * @param lockName
     * @return true:解锁成功，false：锁不存在，或者锁已经超时，
     */
    public Boolean forceUnlock(String lockName) {
        return stringRedisTemplate.execute(forceUnlockScript, Lists.newArrayList(lockName));
    }
}
