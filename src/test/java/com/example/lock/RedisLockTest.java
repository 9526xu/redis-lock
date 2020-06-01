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
        redisLock.unlock("testLockName", "xhh");
    }

    @Test
    public void unlockOtherReentrantKeyFail() {
        // 先用 xhh 加锁
        boolean result = redisLock.tryLock("testLockName", "xhh", 100, TimeUnit.DAYS);
        Assertions.assertEquals(true, result);
        // 在用 xhh1 加锁
        boolean result1 = redisLock.tryLock("testLockName", "xhh1", 100, TimeUnit.DAYS);
        Assertions.assertEquals(false, result1);
        // 使用 xhh1 解锁成功
        Assertions.assertThrows(IllegalMonitorStateException.class, () -> redisLock.unlock("testLockName", "xhh1"));
        // 使用 xhh1 解锁成功
        redisLock.unlock("testLockName", "xhh");


    }


//    @Test
//    public void testUnlockFail() throws InterruptedException {
//        RLock lock = redisson.getLock("lock");
//        Thread t = new Thread() {
//            public void run() {
//                RLock lock = redisson.getLock("lock");
//                lock.lock();
//
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//
//                lock.unlock();
//            }
//
//            ;
//        };
//
//        t.start();
//        t.join(400);
//
//        try {
//            lock.unlock();
//        } catch (IllegalMonitorStateException e) {
//            t.join();
//            throw e;
//        }
//    }


}
