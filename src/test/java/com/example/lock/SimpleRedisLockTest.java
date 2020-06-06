package com.example.lock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/6/6
 */
@SpringBootTest
public class SimpleRedisLockTest {

    @Autowired
    SimpleRedisLock redisLock;

    @Test
    public void lockAndUnlockTest() {
        String lock = "lock";
        String request = UUID.randomUUID().toString();
        Boolean result = redisLock.tryLock(lock, request, 100, TimeUnit.SECONDS);

        Assertions.assertTrue(result);

        Boolean unlock = redisLock.unlock(lock, request);

        Assertions.assertTrue(unlock);
    }

    @Test
    public void unlockFailedTest() {
        String lock = UUID.randomUUID().toString();
        String request = UUID.randomUUID().toString();
        Boolean unlock = redisLock.unlock(lock, request);
        Assertions.assertFalse(unlock);
    }

    @Test
    public void ReentrancyLockTest() {
        String lock = "lock";
        String request = UUID.randomUUID().toString();
        Boolean result = redisLock.tryLock(lock, request, 100, TimeUnit.SECONDS);
        Assertions.assertTrue(result);

        result = redisLock.tryLock(lock, request, 100, TimeUnit.SECONDS);
        Assertions.assertFalse(result);

        Boolean unlock = redisLock.unlock(lock, request);

    }

    @Test
    public void lockOldAndUnlockTest() {

    }

    @Test
    public void testOldUnlock() {

        String lock = "lock";
        String request = UUID.randomUUID().toString();
        Boolean result = redisLock.doOldTryLock(lock, request, 100, TimeUnit.SECONDS);

        Assertions.assertTrue(result);

        Boolean unlock = redisLock.unlock(lock, request);

        Assertions.assertTrue(unlock);
    }




}
