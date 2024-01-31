package com.zou.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {


    private final StringRedisTemplate redisTemplate = null;

    @GetMapping(value = "/health")
    public String health() throws Exception {
        return "UP";
    }
}
