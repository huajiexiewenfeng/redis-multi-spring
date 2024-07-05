[toc]

## 1.前言

本文为大家提供一个 redis 配置多数据源的实现方案；为上一篇文章【https://blog.csdn.net/xiewenfeng520/article/details/140207713】的升级版本

* 支持自定义配置
* 无需写任何其他配置 Java 类
* application.properties 配置完，直接就可以使用

请注意 spring boot 的相关依赖版本。

## 2.git 示例地址

git 仓库地址：https://github.com/huajiexiewenfeng/redis-multi-spring/tree/dev-1.0

上个版本为 master 分支，本文为 dev-1.0 分支升级版本

## 3.需求

1.不需要每一个配置都写一个 Properties 类

2.不需要每一个配置都写一个 RedisTemplate 配置

```java
    @Bean("oneRedisTemplate")
    public RedisTemplate<String, Object> oneRedisTemplate() {...}

    @Bean("twoRedisTemplate")
    public RedisTemplate<String, Object> twoRedisTemplate() {...}
```

3.使用方法可以与 properties 里面的配置直接对应，比如 oneRedisTemplate

```java
    @Autowired
    @Qualifier("oneRedisTemplate")
    private RedisTemplate<String, Object> redisTemplateOne;
```

对应 application.properties 中的 multi.redis.one 前缀

```properties
multi.redis.one.database=${REDIS_DB_INDEX:2}
multi.redis.one.flushdb=${REDIS_FLUSHDB:false}
multi.redis.one.host=${REDIS_HOST:127.0.0.1}
multi.redis.one.port=${REDIS_PORT:6379}
multi.redis.one.password=123456
```

其中，multi.redis.one 前缀中的 one 为自定义字符串，可以为任意值

比如 multi.redis.csdn.database 对应的 spring bean name 为 csdnRedisTemplate

```java
    @Autowired
    @Qualifier("csdnRedisTemplate")
    private RedisTemplate<String, Object> redisTemplateOne;
```

## 4.代码实现

### 4.1 application.properties 配置文件

配置文件和原版保持一致

```properties
spring.application.name=${APPLICATION_NAME:redis-multiple}
server.port=${SERVER_PORT:22216}

# spring 默认配置
spring.redis.database=${REDIS_DB_INDEX:1}
spring.redis.flushdb=${REDIS_FLUSHDB:false}
spring.redis.host=${REDIS_HOST:127.0.0.1}
spring.redis.port=${REDIS_PORT:6379}
spring.redis.password=123456

#第一个 redis 实例配置
multi.redis.one.database=${REDIS_DB_INDEX:2}
multi.redis.one.flushdb=${REDIS_FLUSHDB:false}
multi.redis.one.host=${REDIS_HOST:127.0.0.1}
multi.redis.one.port=${REDIS_PORT:6379}
multi.redis.one.password=123456


#第二个 redis 实例配置
multi.redis.two.database=${REDIS_DB_INDEX:3}
multi.redis.two.flushdb=${REDIS_FLUSHDB:false}
multi.redis.two.host=${REDIS_HOST:127.0.0.1}
multi.redis.two.port=${REDIS_PORT:6379}
multi.redis.two.password=123456
```

### 4.2 获取 application 中的 redis 配置

#### 4.2.1 Environment 对象来获取自定义 redis 配置

实现 EnvironmentAware 接口，获取 Environment 对象，从而来获取我们自定义的配置，核心代码如下：

```java
public class RedisMultiConfiguration implements EnvironmentAware, ApplicationContextAware {
    //...此处省略N行代码
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        // 获取 application.properties 对应的 java 类对象
        PropertySource<?> propertySource = ((StandardServletEnvironment) environment).getPropertySources().get("applicationConfig: [classpath:/application.properties]");
        assert propertySource != null;
        Object source = propertySource.getSource();
        // 拿到 redis 配置的前缀集合,例如 multi.redis.xxx...
        Set<String> redisConfigKeys = new HashSet<>();
        if (source instanceof LinkedHashMap) {
            LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) source;
            Set<String> keys = map.keySet();
            for (String key : keys) {
                // find multi.redis properties
                if (key.startsWith("multi.redis")) {
                    redisConfigKeys.add(key.substring(0, StrUtils.findNthOccurrence(key, ".", 3)));
                }
            }
        }
        if (redisConfigKeys.isEmpty()) {
            log.error("redis config not found");
        }
        this.redisConfigKeyPrefixSet = redisConfigKeys;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    //...此处省略N行代码
}
```

后续再通过  `environment.getProperty(keyPrefix + "." + REDIS_CONFIG_HOST_NAME)` 就可以拿到参数的值

* keyPrefix  = multi.redis.one

### 4.3 初始化 RedisTemplate 对象，并注册到 Spring IOC 容器

#### 4.3.1 初始化方法

```java
@Slf4j
@Configuration
public class RedisMultiConfiguration implements EnvironmentAware, ApplicationContextAware {

    private Environment environment;

    private ApplicationContext applicationContext;

    private Set<String> redisConfigKeyPrefixSet = new HashSet<>();

    @PostConstruct
    public void initConfig() {
        if (redisConfigKeyPrefixSet.isEmpty()) {
            return;
        }
        for (String keyPrefix : redisConfigKeyPrefixSet) {
            String key = keyPrefix.replace(MULTI_REDIS_CONFIG_PREFIX, "");
            // 创建自定义 RedisTemplate 对象
            RedisTemplate<String, Object> redisTemplate = CreateRedisTemplate(keyPrefix, environment);
            // 注册到 Spring IOC 容器
            ((AnnotationConfigServletWebServerApplicationContext) applicationContext).getBeanFactory().registerSingleton(key + "RedisTemplate", redisTemplate);
        }
    }
    //...此处省略N行代码
}
```

#### 4.3.2 CreateRedisTemplate 方法

```java
  private RedisTemplate<String, Object> CreateRedisTemplate(String keyPrefix, Environment environment) {
        JedisConnectionFactory jedisConnectionFactory = this.getJedisConnectionFactory(keyPrefix, environment);
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置key的序列化方式
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setConnectionFactory(jedisConnectionFactory);
        template.setKeySerializer(keySerializer);

        // 设置value的序列化方式
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是无论什么都可以序列化
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 启用DefaultTyping，方便我们反序列化时知道对象的类型
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        valueSerializer.setObjectMapper(om);
        template.setValueSerializer(valueSerializer);
        // 设置Hash的key和value序列化方式
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        // 设置value的泛型类型，这样在存取的时候才会序列化和反序列化成设置的对象类型
        // 注意：这里只是设置了value的泛型，key还是String类型
        template.afterPropertiesSet();
        return template;
    }
```

#### 4.3.3 getJedisConnectionFactory 方法

通过前缀+ environment 去获取对应的配置，再设置到 JedisConnectionFactory 中。

```java
    private JedisConnectionFactory getJedisConnectionFactory(String keyPrefix, Environment environment) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 设置连接池参数，例如最大连接数、最大空闲连接数等
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(30);
        poolConfig.setMinIdle(10);
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(poolConfig);
        jedisConnectionFactory.setHostName(StrUtils.ifEmpty(environment.getProperty(keyPrefix + "." + REDIS_CONFIG_HOST_NAME), "127.0.0.1"));
        jedisConnectionFactory.setPort(Integer.parseInt(StrUtils.ifEmpty(environment.getProperty(keyPrefix + "." + REDIS_CONFIG_PORT_NAME), "6379")));
        jedisConnectionFactory.setDatabase(Integer.parseInt(StrUtils.ifEmpty(environment.getProperty(keyPrefix + "." + REDIS_CONFIG_DATABASE_NAME), "1")));
        jedisConnectionFactory.setPassword(StrUtils.ifEmpty(environment.getProperty(keyPrefix + "." + REDIS_CONFIG_PASSWORD_NAME), "123456"));
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }
```

### 4.4 测试 Demo

```java
@RestController
public class TestController {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    @Qualifier("oneRedisTemplate")
    private RedisTemplate<String, Object> redisTemplateOne;

    @Autowired
    @Qualifier("twoRedisTemplate")
    private RedisTemplate<String, Object> redisTemplateTwo;

    @GetMapping("/test/redis/add")
    public void profileDetails() {
        redisTemplate.opsForValue().set("test1-dev1.0", "1");
        redisTemplateOne.opsForValue().set("test2-dev1.0", 2);
        redisTemplateTwo.opsForValue().set("test3-dev1.0", 3);
    }

}
```

浏览器输入

```html
http://127.0.0.1:22216/test/redis/add
```

执行结果如下：

db1

![image-20240705170741565](https://github.com/huajiexiewenfeng/redis-multi-spring/assets/40154243/7179fd69-6f98-4f1a-af3f-67885165f532)


db2
![image-20240705170754606](https://github.com/huajiexiewenfeng/redis-multi-spring/assets/40154243/05d055d2-bc2b-4d19-bd4a-9c841d4a2bfa)



db3
![image-20240705170807810](https://github.com/huajiexiewenfeng/redis-multi-spring/assets/40154243/688de7c1-1c9f-4be5-9303-9ee22c2b2168)

