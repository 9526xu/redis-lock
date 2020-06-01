package com.example.lock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.Collections;

@SpringBootTest
class RedisLockApplicationTests {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    public void redisTest() {
        stringRedisTemplate.opsForValue().set("test", "111");
        System.out.println(stringRedisTemplate.opsForValue().get("test"));
    }

    /**
     * "if (redis.call('exists', KEYS[1]) == 0) then " +
     * "redis.call('hset', KEYS[1], ARGV[2], 1); " +
     * "redis.call('pexpire', KEYS[1], ARGV[1]); " +
     * "return nil; " +
     * "end; " +
     * "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
     * "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
     * "redis.call('pexpire', KEYS[1], ARGV[1]); " +
     * "return nil; " +
     * "end; " +
     * "return redis.call('pttl', KEYS[1]);",
     */
    @Test
    public void luaTest() {
        String luaScript = "return redis.call('get',KEYS[1])";

        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(String.class);



        String result = stringRedisTemplate.execute(redisScript, Collections.singletonList("foo"));

        System.out.println(result);


//        stringRedisTemplate.execute()

    }

    @Test
    public void redisLockTest() {
        String luaScript = "if (redis.call('exists', KEYS[1]) == 0) then " +
                "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return true; " +
                "end; " +
                "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return true; " +
                "end; " +
                "return false";

        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Boolean.class);

        Boolean result = stringRedisTemplate.execute(redisScript, Collections.singletonList("lock"), "1000000", "thread_2");

        System.out.println(result);

    }

    @Test
    public void unlock() {
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


        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Boolean.class);

        Boolean result = stringRedisTemplate.execute(redisScript, Collections.singletonList("lock"), "thread_2");

        System.out.println(result);

    }

    @Test
    public void luaNilTest() {
        String luaScript = "return nil";

        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(String.class);


        Boolean aLong = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {

            Object nativeConnection = connection.getNativeConnection();
            // 集群模式和单点模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
            // 集群
            if (nativeConnection instanceof JedisCluster) {
                return (Boolean) ((JedisCluster) nativeConnection).eval(luaScript, Collections.emptyList(), Collections.emptyList());
            }

            // 单点
            else if (nativeConnection instanceof Jedis) {
                return (Boolean) ((Jedis) nativeConnection).eval(luaScript, Collections.emptyList(), Collections.emptyList());
            }
            return null;

        });


        String result = stringRedisTemplate.execute(redisScript, Collections.emptyList());

        System.out.println(result);
    }

    @Test
    public void testLua() {
        String lua = "if (redis.call('exists', KEYS[1]) == 0) then " +
                "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return nil; " +
                "end; " +
                "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return nil; " +
                "end; " +
                "return redis.call('pttl', KEYS[1]);";
        System.out.println(lua);
    }

    @Test
    public void evalshaTest() {
        String luaScript = "232fd51614574cf0867b83d384a5e898cfd24e5a";

        stringRedisTemplate.execute((RedisCallback) connection -> {

            Object nativeConnection = connection.getNativeConnection();
            if (nativeConnection instanceof Jedis) {
                Object obj = ((Jedis) nativeConnection).evalsha(luaScript);
                System.out.println(obj);
            }
            return null;

        });

    }


}
