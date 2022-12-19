package com.vsware.libraries.redisreactivecache.ports;

import reactor.core.publisher.Mono;

/**
 * @author hazem
 */
public interface CachePort {
    Mono<Void> set(String key, Object value);

    Mono<Void> delete(String key);

    Mono<Object> get(String key);

    Mono<Void> flushAll();
}
