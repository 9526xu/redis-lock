## redis 分布式锁

欢迎微信关注**程序通事**

![](https://tva1.sinaimg.cn/large/007S8ZIlly1gfryjmj0u9j30p00dwtaq.jpg)

## 分布式锁

[基于 Redis 分布式锁设计实现](https://studyidea.cn/redis-lock)  

分布式锁代码请参考：[SimpleRedisLock](src/main/java/com/example/lock/SimpleRedisLock.java)

## 可重入分布式锁

[可重入分布式锁设计实现](https://studyidea.cn/redis-lock2)


基于 Redis Hash 可重入分布式锁请参考：[RedisReentrancyLock](src/main/java/com/example/lock/RedisReentrancyLock.java)

若**spring-data-redis** 低于 2.1.9,且底层使用 Jedis 连接，另外 Redis 使用 cluster 几圈，可重入分布式锁请参考：
[RedisReentrancyLock](src/main/java/com/example/lock/old/RedisReentrancyLock.java)

基于 `ThreadLocal` 可重入分布式锁请参考：[RedisReentrancyLock](src/main/java/com/example/lock/RedisReentrancyThreadLocalLock.java)








 