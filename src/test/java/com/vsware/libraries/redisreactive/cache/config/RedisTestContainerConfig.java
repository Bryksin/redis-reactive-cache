package com.vsware.libraries.redisreactive.cache.config;

import com.github.javafaker.Faker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
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
        RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration(redisContainer.getHost(),
                redisContainer.getMappedPort(6379));
        return new LettuceConnectionFactory(redisConf);
    }

    @Bean
    public Faker faker() {
        return new Faker(Locale.ENGLISH);
    }
}
