package com.vsware.libraries.redisreactive.cache.adapters;

import com.vsware.libraries.redisreactive.cache.ports.CachePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author hazem
 */
@Component
@Profile("in-memory-test")
public class InMemoryCache implements CachePort {
    private final Map<String, Object> cachedItems = new HashMap<>();
    private boolean hasError = false;

    @Override
    public Mono<Void> set(String key, Object value) {
        if (hasError) return Mono.error(new RuntimeException("Fake error"));
        cachedItems.put(key, value);
        return Mono.empty();
    }

    @Override
    public Mono<Void> set(String key, Object value, int ttl, TimeUnit timeUnit) {
        return set(key, value);
    }

    @Override
    public Mono<Void> delete(String key) {
        if (hasError) return Mono.error(new RuntimeException("Fake error"));
        cachedItems.remove(key);
        return Mono.empty();
    }

    @Override
    public Mono<Object> get(String key) {
        if (hasError) return Mono.error(new RuntimeException("Fake error"));
        return cachedItems.containsKey(key) ? Mono.just(cachedItems.get(key)) : Mono.empty();
    }

    @Override
    public Mono<Void> flushAll() {
        if (hasError) return Mono.error(new RuntimeException("Fake error"));
        cachedItems.clear();
        return Mono.empty();
    }

    public void forceError() {
        hasError = true;
    }

    public void reset() {
        hasError = false;
    }


}
