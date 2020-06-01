package com.example.lock;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/5/23
 */
@Service
public class RedisLock {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript lockScript;

    private DefaultRedisScript unlockScript;
    @PostConstruct
    public void init() {
        String lockLuaScript = "if (redis.call('exists', KEYS[1]) == 0) then\n" +
                "    redis.call('hincrby', KEYS[1], ARGV[2], 1);\n" +
                "    redis.call('pexpire', KEYS[1], ARGV[1]);\n" +
                "    return true;\n" +
                "end ;\n" +
                "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then\n" +
                "    redis.call('hincrby', KEYS[1], ARGV[2], 1);\n" +
                "    redis.call('pexpire', KEYS[1], ARGV[1]);\n" +
                "    return true;\n" +
                "end ;\n" +
                "return false;";

        lockScript = new DefaultRedisScript();
        lockScript.setScriptText(lockLuaScript);

        String unlockLuaScript = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then\n" +
                "    return nil;\n" +
                "end ;\n" +
                "local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1);\n" +
                "if (counter > 0) then\n" +
                "    return false;\n" +
                "else\n" +
                "    redis.call('del', KEYS[1]);\n" +
                "    return true;\n" +
                "end ;\n" +
                "return nil;";

        unlockScript = new DefaultRedisScript();
        unlockScript.setScriptText(unlockLuaScript);
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
    public boolean tryLock(String lockName, String reentrantKey, long leaseTime, TimeUnit unit) {
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
        if (obj == null) {
            return null;
        }
        return Long.valueOf(1).equals(obj) || "OK".equals(obj);
    }
}
