package com.example.lock;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/6/6
 * <p>
 * 简单的分布式非阻塞式锁实现,优化解锁，使其无法释放其他应用/线程的加的锁
 * 缺点：
 * 1. 无法重入
 */
@Service
public class SimpleRedisLock {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 非阻塞式加锁，若锁存在，直接返回
     *
     * @param lockName  锁名称
     * @param request   唯一标识，防止其他应用/线程解锁，可以使用 UUID 生成
     * @param leaseTime 超时时间
     * @param unit      时间单位
     * @return
     */
    public Boolean tryLock(String lockName, String request, long leaseTime, TimeUnit unit) {
        // 注意该方法是在 spring-boot-starter-data-redis 2.1 版本新增加的，若是之前版本 可以执行下面的方法
        return stringRedisTemplate.opsForValue().setIfAbsent(lockName, request, leaseTime, unit);
    }

    /**
     * 适用于 spring-boot-starter-data-redis 2.1 之前的版本
     *
     * @param lockName
     * @param request
     * @param leaseTime
     * @param unit
     * @return
     */
    private Boolean doOldTryLock(String lockName, String request, long leaseTime, TimeUnit unit) {
        long internalLockLeaseTime = unit.toMillis(leaseTime);
        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            RedisSerializer valueSerializer = stringRedisTemplate.getValueSerializer();
            RedisSerializer keySerializer = stringRedisTemplate.getKeySerializer();
            Object obj = connection.execute("set", keySerializer.serialize(lockName),
                    valueSerializer.serialize(request),
                    "NX".getBytes(StandardCharsets.UTF_8),
                    "PX".getBytes(StandardCharsets.UTF_8),
                    String.valueOf(internalLockLeaseTime).getBytes(StandardCharsets.UTF_8));
            return obj != null;
        });
        return result;
    }

    public Boolean unlock(String lockName, String request) {
        DefaultRedisScript<Boolean> unlockScript = new DefaultRedisScript<>();
        unlockScript.setLocation(new ClassPathResource("simple_unlock.lua"));
        unlockScript.setResultType(Boolean.class);
        return stringRedisTemplate.execute(unlockScript, Lists.newArrayList(lockName), request);
    }

}
