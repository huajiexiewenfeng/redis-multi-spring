package com.csdn.rm.config;

import com.csdn.rm.StrUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.context.support.StandardServletEnvironment;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import static com.csdn.rm.Constants.*;

/**
 * @Author: xiewenfeng
 * @Date: 2024/7/5 15:30
 */
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
            RedisTemplate<String, Object> redisTemplate = CreateRedisTemplate(keyPrefix, environment);
            ((AnnotationConfigServletWebServerApplicationContext) applicationContext).getBeanFactory().registerSingleton(key + "RedisTemplate", redisTemplate);
        }
    }

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


    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        PropertySource<?> propertySource = ((StandardServletEnvironment) environment).getPropertySources().get("applicationConfig: [classpath:/application.properties]");
        assert propertySource != null;
        Object source = propertySource.getSource();
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
}
