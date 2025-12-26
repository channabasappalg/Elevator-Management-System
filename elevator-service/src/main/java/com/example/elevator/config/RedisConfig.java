package com.example.elevator.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {
    // Spring Boot auto-configuration handles Redis connection details from application.properties
}