# redis-boot

## preview

![](https://github.com/zljin/hexo/blob/master/image_address/redis_action_list.png?raw=true)


## mission 1: 短信登录，Redis的共享session应用逻辑

1. 短信验证

通过手机号为key生成对于的随机码为value,登录的时候从缓存拿取校验

2. 生成用户token逻辑

校验成功后,根据此用户信息生成独特的tokenKey,value以Map的形式存放用户信息，并设置ttl

3. RefreshToken逻辑

添加拦击器，通过传入的header的tokenKey信息，去redis拿用户数据是否失效，若用户存在，刷新token有效期.
并将用户信息塞入到UserHolder工具类(ThreadLocal)中,在当前线程都可通过此工具类获取用户信息


## mission 2: 商户查询缓存,缓存穿透与雪崩

查询的key在redis不存在,对应在数据库为空,此时若大量请求,则不经过缓存,直接访问db,操作多次IO,影响数据库性能。

如何解决？
1. 若查询结果为空,把空的查询结果的数据缓存起来,下次直接return
2. 布隆过滤器

```
https://juejin.cn/post/6844903982209449991#heading-6
把字符串abc的每个字符通过k个哈希函数映射到二进制数组的k个点，设置为1，检索时，每个点都为1则证明该元素存在，否则被检元素不存在
缺点：存在误判，删除困难，可以控制容错率参数百分比
```

### 缓存雪崩

> Redis里面key值大面积失效,此时有大量的流量访问系统,大量流量直接访问数据库导致数据库宕机

解决：1. 永不过期,手动失效 2. 过期时间错开 3. 多缓存结合（Redis->memcache->database）
   

## mission 3: 压测RedisIdWorker能够生成唯一id
## mission 4: setnx 分布式锁处理秒杀

```
原理:
setnx lockkey lockvalue  #执行成功,获得该锁,往下执行
expire lockkey 30 # 防死锁
{
    #业务代码段
}
del lockkey #执行结束,删除锁
# 其他进程继续竞争抢锁

如果临界值执行时间过长,在超时之内没有执行完也会有并发问题
1. 延长超时时间 2. 定时判断该线程是否执行结束,否则续上时间
```

## mission 5: 查询附近的商户 Geospatial

## mission 6: 网站访问量 HyperLogLog
> 适用于快速合并数据去重，允许一定误差值，效率很高

## mission 7: 达人探店，点赞排行榜 ZSet

## mission 8: 用户共同关注，用Set数据结构，进行集合计算

## mission 9: 批处理方案

1. 原生M操作
2. pipeline批处理（支持多种数据类型）

> 将一组 Redis 命令进行组装，通过一次传输给 Redis 并返回结果集(只最后执行一次，不能在执行期间查结果)

tip:
1. 不建议一次批处理传输太多命令，占用带宽，网络阻塞
2. pipeline不具备原子性


