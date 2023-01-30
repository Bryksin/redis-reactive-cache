package com.vsware.libraries.redisreactive.cache.config;

import com.github.javafaker.Faker;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Locale;

@Configuration
@Testcontainers
public class RedisTestContainerConfig {

    @Container
    public static GenericContainer redisContainer = new GenericContainer(DockerImageName.parse("redis:6.2.6-alpine"))
            .withExposedPorts(6379);

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        redisContainer.start();
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+redisContainer.getHost()+":"+redisContainer.getMappedPort(6379));
        
        return new RedissonConnectionFactory(Redisson.create(config));
    }

    @Bean
    public Faker faker() {
        return new Faker(Locale.ENGLISH);
    }
}
