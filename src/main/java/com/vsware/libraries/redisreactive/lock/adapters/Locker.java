package com.vsware.libraries.redisreactive.lock.adapters;

import com.vsware.libraries.redisreactive.cache.ports.CachePort;
import com.vsware.libraries.redisreactive.lock.erros.LockInternalError;
import com.vsware.libraries.redisreactive.lock.erros.ResourceAlreadyLockedError;
import com.vsware.libraries.redisreactive.lock.ports.LockerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * @author Bilel
 */
@Component
@RequiredArgsConstructor
public class Locker implements LockerPort {

    private final CachePort cache;

    @Override
    public Mono<Void> lock(String resourceKey, String fencingKey, int ttl, TimeUnit timeUnit) {
        return getCache(resourceKey)
                .flatMap(cacheResponse -> {
                    if (cacheResponse.equals(fencingKey)) {
                        return Mono.error(ResourceAlreadyLockedError::new);
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(cache.set(resourceKey, fencingKey, ttl, timeUnit))
                .onErrorResume(throwable -> Mono.error(new LockInternalError(throwable.getMessage())))
                .then();
    }

    @Override
    public Mono<Void> unlock(String resourceKey, String fencingKey) {
        return getCache(resourceKey)
                .flatMap(cacheResponse -> {
                    if (cacheResponse.equals(fencingKey)) {
                        return cache.delete(resourceKey);
                    }
                    return Mono.empty();
                }).then();
    }
    
    private Mono<String> getCache(String resourceKey) {
        return this.cache.get(resourceKey)
                .map(String::valueOf)
                .onErrorResume(throwable -> Mono.error(new LockInternalError(throwable.getMessage())));
    }
}
