package com.example.lock;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.concurrent.TimeUnit;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/5/23
 */
@Service
public class RedisLock {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

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

        String luaScript = "if (redis.call('exists', KEYS[1]) == 0) then\n" +
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

        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {

            Object nativeConnection = connection.getNativeConnection();
            // 集群模式和单点模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
            // 集群
            Object innerResult = null;
            if (nativeConnection instanceof JedisCluster) {
                innerResult = ((JedisCluster) nativeConnection).eval(luaScript, Lists.newArrayList(lockName), Lists.newArrayList(String.valueOf(internalLockLeaseTime), reentrantKey));
            }

            // 单点
            else if (nativeConnection instanceof Jedis) {
                innerResult = ((Jedis) nativeConnection).eval(luaScript, Lists.newArrayList(lockName), Lists.newArrayList(String.valueOf(internalLockLeaseTime), reentrantKey));
            }
            return convert(innerResult);
        });
        return result;
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

        String luaScript = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then\n" +
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


        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {

            Object nativeConnection = connection.getNativeConnection();
            // 集群模式和单点模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
            // 集群
            Object innerResult = null;
            if (nativeConnection instanceof JedisCluster) {
                innerResult = ((JedisCluster) nativeConnection).eval(luaScript, Lists.newArrayList(lockName), Lists.newArrayList(reentrantKey));
            }

            // 单点
            else if (nativeConnection instanceof Jedis) {
                innerResult = ((Jedis) nativeConnection).eval(luaScript, Lists.newArrayList(lockName), Lists.newArrayList(reentrantKey));
            }
            return convert(innerResult);
        });

        if (result == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by lockName:+" + lockName + " with reentrantKey: "
                    + reentrantKey);
        }
    }

    public Boolean convert(Object obj) {
        if (obj == null) {
            return null;
        }
        return Long.valueOf(1).equals(obj) || "OK".equals(obj);
    }
}
