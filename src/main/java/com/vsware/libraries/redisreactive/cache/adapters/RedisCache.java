package com.vsware.libraries.redisreactive.cache.adapters;

import com.vsware.libraries.redisreactive.cache.ports.CachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author hazem
 */
@ConditionalOnClass({ReactiveRedisTemplate.class})
@Component
@RequiredArgsConstructor
@Profile("!in-memory-test")
public class RedisCache implements CachePort {
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Override
    public Mono<Void> set(String key, Object value) {
        return reactiveRedisTemplate.opsForValue().set(key, value)
                .then();
    }

    @Override
    public Mono<Void> set(String key, Object value, int ttl, TimeUnit timeUnit) {
        return reactiveRedisTemplate.opsForValue().set(key, value, Duration.of(ttl, timeUnit.toChronoUnit()))
                .then();
    }

    @Override
    public Mono<Void> delete(String key) {
        return reactiveRedisTemplate.opsForValue().delete(key)
                .then();
    }

    @Override
    public Mono<Object> get(String key) {
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    @Override
    public Mono<Void> flushAll() {
        return reactiveRedisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll()
                .then();
    }
}
