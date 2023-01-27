package com.vsware.libraries.redisreactive.cache.ports;

import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * @author hazem
 */
public interface CachePort {
    Mono<Void> set(String key, Object value);
    
    Mono<Void> set(String key, Object value, int ttl, TimeUnit timeUnit);

    Mono<Void> delete(String key);

    Mono<Object> get(String key);

    Mono<Void> flushAll();
}
