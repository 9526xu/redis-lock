---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by andy.xu.
--- DateTime: 2020/5/23 16:41
---

-- 判断 hash set 可重入 key 的值是否等于 0
-- 如果为 0 代表 该可重入 key 不存在
if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then
    return nil;
end ;
-- 计算当前可重入次数
local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1);
-- 小于等于 0 代表可以解锁
if (counter > 0) then
    return false;
else
    redis.call('del', KEYS[1]);
    return true;
end ;
return nil;


