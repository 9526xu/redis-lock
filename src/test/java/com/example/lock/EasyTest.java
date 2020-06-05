package com.example.lock;

import lombok.Data;
import org.junit.jupiter.api.Test;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/5/23
 */
public class EasyTest {

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
