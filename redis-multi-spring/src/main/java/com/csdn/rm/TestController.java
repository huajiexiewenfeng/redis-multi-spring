package com.csdn.rm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
