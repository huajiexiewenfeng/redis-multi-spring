package com.csdn.rm.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @Author: xiewenfeng
 * @Date: 2022/4/14 15:30
 */
@Configuration
public class RedisMultiConfiguration {

    @Autowired
    private RedisOneProperties properties;

    @Autowired
    private RedisTwoProperties propertiesTwo;

    // 第一个Redis服务器的配置
    @Bean("oneRedisTemplate")
    public RedisTemplate<String, Object> oneRedisTemplate() {
        JedisConnectionFactory jedisConnectionFactory = this.getJedisConnectionFactory();
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

    private JedisConnectionFactory getJedisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 设置连接池参数，例如最大连接数、最大空闲连接数等
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(30);
        poolConfig.setMinIdle(10);
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(poolConfig);
        jedisConnectionFactory.setHostName(properties.getHost());
        jedisConnectionFactory.setPort(properties.getPort());
        jedisConnectionFactory.setDatabase(properties.getDatabase());
        jedisConnectionFactory.setPassword(properties.getPassword());
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }

    // 第一个Redis服务器的配置
    @Bean("twoRedisTemplate")
    public RedisTemplate<String, Object> twoRedisTemplate() {
        JedisConnectionFactory jedisConnectionFactory = this.getJedisConnectionFactoryTwo();
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

    private JedisConnectionFactory getJedisConnectionFactoryTwo() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 设置连接池参数，例如最大连接数、最大空闲连接数等
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(30);
        poolConfig.setMinIdle(10);
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(poolConfig);
        jedisConnectionFactory.setHostName(propertiesTwo.getHost());
        jedisConnectionFactory.setPort(propertiesTwo.getPort());
        jedisConnectionFactory.setDatabase(propertiesTwo.getDatabase());
        jedisConnectionFactory.setPassword(propertiesTwo.getPassword());
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }
}
