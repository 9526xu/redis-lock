package com.example.lock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/5/23
 */
@SpringBootTest
@Service
public class RedisLockTest {

    @Autowired
    RedisLock redisLock;

    @Test
    public void lockAndUnlockTest() {
        boolean result = redisLock.tryLock("testLockName", "xhh", 100, TimeUnit.DAYS);
        Assertions.assertEquals(true, result);
        redisLock.unlock("testLockName","xhh");
    }


}
