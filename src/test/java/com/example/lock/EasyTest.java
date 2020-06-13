package com.example.lock;

import com.google.common.collect.Lists;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.script.DigestUtils;
import redis.clients.jedis.Jedis;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/5/23
 */
public class EasyTest {

    @Test
    public void jedisTest() {
        //连接本地的 Redis 服务
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.auth("1234qwer");

        System.out.println("服务正在运行: " + jedis.ping());

        String lua_script = "return redis.call('set',KEYS[1],ARGV[1])";
        String lua_sha1 = DigestUtils.sha1DigestAsHex(lua_script);

        try {
            Object evalsha = jedis.evalsha(lua_sha1, Lists.newArrayList("foo"), Lists.newArrayList("楼下小黑哥"));
        } catch (Exception e) {
            Throwable current = e;
            while (current != null) {
                String exMessage = current.getMessage();
                // 包含 NOSCRIPT，代表该 lua 脚本从未被执行，需要先执行 eval 命令
                if (exMessage != null && exMessage.contains("NOSCRIPT")) {
                    Object eval = jedis.eval(lua_script, Lists.newArrayList("foo"), Lists.newArrayList("楼下小黑哥"));
                    break;
                }

            }
        }
        String foo = jedis.get("foo");
        System.out.println(foo);


    }

    @Test
    public void jedisStatusTest() {
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.auth("1234qwer");


        String str = jedis.get("test1111");
        String set = jedis.set("test", "111");
        Boolean test = jedis.exists("test");



    }


    @Test
    public void test() {
        boolean flag = true; //设置成true，保证表达式 2 被执行
        Integer nullInteger = null;
        Long objLong = Long.valueOf(88l);

        Object result = Long.valueOf(flag ? (long) nullInteger.intValue() : objLong.longValue());
//        Redisson redisson = new Redisson();
//        RLock redissonLock = redisson.getLock("asda");

    }


    public static Integer testObject() {
        //  private Integer code;
        SimpleObj simpleObj = new SimpleObj();
        // 其他业务逻辑

//        if (simpleObj == null) {
//            return -1;
//        } else {
//            return simpleObj.getCode();
//        }

        return simpleObj == null ? -1 : simpleObj.getCode();
    }

    @Test
    public void testObjectCondition() {
        System.out.println(testObject());
    }

    @Data
    public static class SimpleObj {
        private Integer code;
    }

    private Integer objectFoo() {
        return null;

    }

    private int simpleFoo() {
        return 66;
    }

    private boolean someCondition() {
        return true;
    }
}
