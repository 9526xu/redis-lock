package com.example.lock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
        boolean result = redisLock.tryReentrancyLock("testLockName", "xhh", 100, TimeUnit.DAYS);
        Assertions.assertEquals(true, result);
        redisLock.unlock("testLockName", "xhh");
    }

    @Test
    public void unlockOtherReentrantKeyFail() {
        // 先用 xhh 加锁
        boolean result = redisLock.tryReentrancyLock("testLockName", "xhh", 100, TimeUnit.DAYS);
        Assertions.assertEquals(true, result);
        // 在用 xhh1 加锁
        boolean result1 = redisLock.tryReentrancyLock("testLockName", "xhh1", 100, TimeUnit.DAYS);
        Assertions.assertEquals(false, result1);
        // 使用 xhh1 解锁成功
        Assertions.assertThrows(IllegalMonitorStateException.class, () -> redisLock.unlock("testLockName", "xhh1"));
        // 使用 xhh1 解锁成功
        redisLock.unlock("testLockName", "xhh");
    }

    @Test
    public void directUnlockFailed() {
        Assertions.assertThrows(IllegalMonitorStateException.class, () -> redisLock.unlock("test11", UUID.randomUUID().toString()));
    }


    @Test
    public void testLockUnlock() {
        String lockName = UUID.randomUUID().toString();
        String reentrentKey = UUID.randomUUID().toString();

        boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey, 100, TimeUnit.DAYS);
        Assertions.assertEquals(true, result);
        redisLock.unlock(lockName, reentrentKey);


        result = redisLock.tryReentrancyLock(lockName, reentrentKey, 100, TimeUnit.DAYS);
        Assertions.assertEquals(true, result);
        redisLock.unlock(lockName, reentrentKey);
    }

    @Test
    public void testReentrancy() throws InterruptedException {
        String lockName = UUID.randomUUID().toString();
        String reentrentKey = UUID.randomUUID().toString();

        boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey, 100, TimeUnit.MINUTES);
        Assertions.assertEquals(true, result);
        result = redisLock.tryReentrancyLock(lockName, reentrentKey, 100, TimeUnit.MINUTES);
        Assertions.assertEquals(true, result);
        // 解锁 redis 一次
        redisLock.unlock(lockName, reentrentKey);
        // next row  for test renew expiration tisk.
        //Thread.currentThread().sleep(TimeUnit.SECONDS.toMillis(RedissonLock.LOCK_EXPIRATION_INTERVAL_SECONDS*2));
        Thread thread1 = new Thread() {
            @Override
            public void run() {
                boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey+Thread.currentThread().getName(), 100, TimeUnit.MINUTES);
                Assertions.assertEquals(false, result);
            }
        };
        thread1.start();
        thread1.join();
        // 解锁 redis 一次
        redisLock.unlock(lockName, reentrentKey);
    }


    @Test
    public void testIsLockedOtherThread() throws InterruptedException {

        String lockName = UUID.randomUUID().toString();
        String reentrentKey = UUID.randomUUID().toString();

        boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey, 100, TimeUnit.MINUTES);
        Assertions.assertEquals(true, result);

        Thread t = new Thread() {
            public void run() {
                boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey+Thread.currentThread().getName(), 100, TimeUnit.MINUTES);
                Assertions.assertEquals(false, result);
            };
        };

        t.start();
        t.join();

        // 解锁 redis 一次
        redisLock.unlock(lockName, reentrentKey);

        Thread t2 = new Thread() {
            public void run() {
                boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey+Thread.currentThread().getName(), 100, TimeUnit.MINUTES);
                Assertions.assertEquals(true, result);
                // 解锁 redis 一次
                redisLock.unlock(lockName, reentrentKey+Thread.currentThread().getName());
            };
        };

        t2.start();
        t2.join();
    }

    @Test
    public void testIsLocked() {
        String lockName = UUID.randomUUID().toString();
        String reentrentKey = UUID.randomUUID().toString();

        Assertions.assertFalse(redisLock.isLocked(lockName));
        boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey, 100, TimeUnit.MINUTES);
        Assertions.assertEquals(true, result);
        Assertions.assertTrue(redisLock.isLocked(lockName));

        // 解锁 redis 一次
        redisLock.unlock(lockName, reentrentKey);


        Assertions.assertFalse(redisLock.isLocked(lockName));
    }

    @Test
    public void testUnlockFail() throws InterruptedException {

        String lockName = UUID.randomUUID().toString();
        String reentrentKey = UUID.randomUUID().toString();




        Thread t = new Thread() {
            public void run() {
                boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey+Thread.currentThread().getName(), 100, TimeUnit.MINUTES);
                Assertions.assertEquals(true, result);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // 解锁 redis 一次
                redisLock.unlock(lockName, reentrentKey+Thread.currentThread().getName());
            };
        };

        t.start();
        t.join(400);
        Assertions.assertThrows(IllegalMonitorStateException.class, () -> redisLock.unlock(lockName, reentrentKey));
        t.join();
    }

    @Test
    public void testConcurrency_MultiInstance() throws InterruptedException {
        int iterations = 100;
        final AtomicInteger lockedCounter = new AtomicInteger();

        String lockName = UUID.randomUUID().toString();
        String reentrentKey = UUID.randomUUID().toString();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);


        CyclicBarrier cyclicBarrier=new CyclicBarrier(100);
        CountDownLatch countDownLatch=new CountDownLatch(100);

        for (int i = 0; i <iterations ; i++) {
            executor.submit(()->{
                boolean result = redisLock.tryReentrancyLock(lockName, reentrentKey, 100, TimeUnit.MINUTES);
                Assertions.assertEquals(true, result);
                lockedCounter.incrementAndGet();
                // 解锁 redis 一次
                redisLock.unlock(lockName, reentrentKey);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();



        Assertions.assertFalse(redisLock.isLocked(lockName));
        Assertions.assertEquals(iterations,lockedCounter.get());
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
