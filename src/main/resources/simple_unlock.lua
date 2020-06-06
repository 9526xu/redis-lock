-- 解锁代码
-- 首先判断传入的唯一标识是否与现有标识一致
-- 如果一致，释放这个锁，否则直接返回
if redis.call('get', KEYS[1]) == ARGV[1] then
	return redis.call('del', KEYS[1])
else
	return 0
end