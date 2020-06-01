## redis 分布式锁

## 注意点

### 类型转换问题

从 Lua 转换到 Redis：

Lua number -> Redis integer reply / Lua 数字转换成 Redis 整数
Lua string -> Redis bulk reply / Lua 字符串转换成 Redis bulk 回复
Lua table (array) -> Redis multi bulk reply / Lua 表(数组)转换成 Redis 多条 bulk 回复
Lua table with a single ok field -> Redis status reply / 一个带单个 ok 域的 Lua 表，转换成 Redis 状态回复
Lua table with a single err field -> Redis error reply / 一个带单个 err 域的 Lua 表，转换成 Redis 错误回复
Lua boolean false -> Redis Nil bulk reply / Lua 的布尔值 false 转换成 Redis 的 Nil bulk 回复
从 Lua 转换到 Redis 有一条额外的规则，这条规则没有和它对应的从 Redis 转换到 Lua 的规则：

Lua boolean true -> Redis integer reply with value of 1 / Lua 布尔值 true 转换成 Redis 整数回复中的 1

例子：

![](https://tva1.sinaimg.cn/large/007S8ZIlly1gf2jibpln1j311008udgi.jpg)

### springboot redisTemplate 不支持 cluster

**性能优化**

```java
this.sha1 = DigestUtils.sha1DigestAsHex(getScriptAsString());
```

![](https://imgkr.cn-bj.ufileos.com/8258cbfc-6a6d-408f-9c6c-0b33326cbb64.png)


[issue](https://jira.spring.io/browse/DATAREDIS-1005)

 