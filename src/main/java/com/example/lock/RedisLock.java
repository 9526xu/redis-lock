package com.example.lock;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/5/23
 */
@Slf4j
@Service
public class RedisLock {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript lockScript;

    private DefaultRedisScript unlockScript;

    @PostConstruct
    private void init() {
        try {
            String lockLuaScript = IOUtils.toString(ResourceUtils.getURL("classpath:lock.lua").openStream());
            lockScript = new DefaultRedisScript();
            lockScript.setScriptText(lockLuaScript);
            String unlockLuaScript = IOUtils.toString(ResourceUtils.getURL("classpath:unlock.lua").openStream());
            unlockScript = new DefaultRedisScript();
            unlockScript.setScriptText(unlockLuaScript);
        } catch (IOException e) {
            log.error("RedisLockService init failed", Throwables.getStackTraceAsString(e));
        }
    }


    /**
     * 可重入锁
     *
     * @param lockName     锁名字,代表需要争临界资源
     * @param reentrantKey 可重入 key
     * @param leaseTime    锁释放时间
     * @param unit         锁释放时间单位
     * @return
     */
    public boolean tryReentrancyLock(String lockName, String reentrantKey, long leaseTime, TimeUnit unit) {
        long internalLockLeaseTime = unit.toMillis(leaseTime);
        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            Object innerResult = eval(connection.getNativeConnection(), lockScript, Lists.newArrayList(lockName), Lists.newArrayList(String.valueOf(internalLockLeaseTime), reentrantKey));
            return convert(innerResult);
        });
        return result;
    }

    private Object evalBySingle(Jedis jedis, RedisScript redisScript, final List<String> keys, final List<String> args) {
        // 先执行 evalsha
        Object innerResult;

        try {
            innerResult = jedis.evalsha(redisScript.getSha1(), keys, args);
        } catch (Exception e) {

            if (!exceptionContainsNoScriptError(e)) {
                throw e instanceof RuntimeException ? (RuntimeException) e : new RedisSystemException(e.getMessage(), e);
            }
            innerResult = jedis.eval(redisScript.getScriptAsString(), keys, args);
        }
        return innerResult;
    }

    private Object evalByCluster(JedisCluster jedisCluster, RedisScript redisScript, final List<String> keys, final List<String> args) {
        // 先执行 evalsha
        Object innerResult;

        try {
            innerResult = jedisCluster.evalsha(redisScript.getSha1(), keys, args);
        } catch (Exception e) {
            if (!exceptionContainsNoScriptError(e)) {
                throw e instanceof RuntimeException ? (RuntimeException) e : new RedisSystemException(e.getMessage(), e);
            }
            innerResult = jedisCluster.eval(redisScript.getScriptAsString(), keys, args);
        }
        return innerResult;


    }


    private boolean exceptionContainsNoScriptError(Throwable e) {

        Throwable current = e;
        while (current != null) {

            String exMessage = current.getMessage();
            if (exMessage != null && exMessage.contains("NOSCRIPT")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    /**
     * 解锁
     * 若可重入 key 次数大于 1，将可重入 key 次数减 1
     *
     * @param lockName
     * @param reentrantKey
     * @throws IllegalMonitorStateException 解锁之前，请先加锁。若为加锁，解锁将会抛出该错误
     */
    public void unlock(String lockName, String reentrantKey) {
        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            Object innerResult = eval(connection.getNativeConnection(), unlockScript, Lists.newArrayList(lockName), Lists.newArrayList(reentrantKey));
            // 返回 null 代表解锁失败  1 代表 解锁成功，lock 被删除  0 代表可重入key 减1
            return convert(innerResult);
        });

        if (result == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by lockName:+" + lockName + " with reentrantKey: "
                    + reentrantKey);
        }
    }

    private Object eval(Object nativeConnection, RedisScript redisScript, final List<String> keys, final List<String> args) {

        Object innerResult = null;
        // 集群模式和单点模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
        // 集群
        if (nativeConnection instanceof JedisCluster) {
            innerResult = evalByCluster((JedisCluster) nativeConnection, redisScript, keys, args);
        }
        // 单点
        else if (nativeConnection instanceof Jedis) {
            innerResult = evalBySingle((Jedis) nativeConnection, redisScript, keys, args);
        }
        return innerResult;
    }

    public Boolean convert(Object obj) {
        Long result;
        if (obj == null) {
            return null;
        } else {
            result = Long.valueOf(obj.toString());
        }
        // redis 返回 1 代表 成功
        return result.compareTo(0L) > 0;
    }

    public boolean isLocked(String lockName) {
        return stringRedisTemplate.hasKey(lockName);
    }
}
