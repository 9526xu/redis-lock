if (redis.call('del', KEYS[1]) == 1) then

     return 1
else
    return 0

end