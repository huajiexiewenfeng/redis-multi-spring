package com.csdn.rm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: xiewenfeng
 * @CreateTime: 2024/7/5 16:34
 * @Version: 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "multi.redis.two")
@Data
public class RedisTwoProperties {

    private String host;

    private int port;

    private int database = 1;

    private String password;

}
