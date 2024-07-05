# redis 多实例配置
1.不需要每一个配置都写一个 RedisOneProperties 类

2.不需要每一个配置都写一个 RedisTemplate 配置

```java
    @Bean("oneRedisTemplate")
    public RedisTemplate<String, Object> oneRedisTemplate() {}

    @Bean("twoRedisTemplate")
    public RedisTemplate<String, Object> twoRedisTemplate() {}
```

3.使用方法可以与 properties 里面的直接对应

```java
    @Autowired
    @Qualifier("oneRedisTemplate")
    private RedisTemplate<String, Object> redisTemplateOne;
```

对应 application.properties 中的

```properties
multi.redis.one.database=${REDIS_DB_INDEX:2}
multi.redis.one.flushdb=${REDIS_FLUSHDB:false}
multi.redis.one.host=${REDIS_HOST:10.172.198.38}
multi.redis.one.port=${REDIS_PORT:6379}
multi.redis.one.password=NLK2Gi7Ot.D
```

如果有时间，会在下篇文章中实现。