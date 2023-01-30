package com.vsware.libraries.redisreactive.lock.adapters;

import com.vsware.libraries.redisreactive.lock.erros.LockInternalError;
import com.vsware.libraries.redisreactive.lock.erros.ResourceAlreadyLockedError;
import com.vsware.libraries.redisreactive.lock.ports.LockerPort;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucketReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * @author Bilel
 */
@Component
@RequiredArgsConstructor
public class Locker implements LockerPort {

    private final RedissonReactiveClient redissonReactiveClient;

    @Override
    public Mono<Void> lock(String resourceKey, String fencingKey, int ttl, TimeUnit timeUnit) {
        RBucketReactive<String> bucket = redissonReactiveClient.getBucket(resourceKey, StringCodec.INSTANCE);
        return getCache(resourceKey)
                .flatMap(cacheResponse -> {
                    if (cacheResponse.equals(fencingKey)) {
                        return Mono.error(ResourceAlreadyLockedError::new);
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(bucket.set(fencingKey, ttl, timeUnit))
                .onErrorResume(throwable -> Mono.error(new LockInternalError(throwable.getMessage())))
                .then();
    }

    @Override
    public Mono<Void> unlock(String resourceKey, String fencingKey) {
        RBucketReactive<String> bucket = redissonReactiveClient.getBucket(resourceKey, StringCodec.INSTANCE);
        return getCache(resourceKey)
                .flatMap(cacheResponse -> {
                    if (cacheResponse.equals(fencingKey)) {
                        return bucket.delete();
                    }
                    return Mono.empty();
                }).then();
    }
    
    private Mono<String> getCache(String resourceKey) {
        RBucketReactive<String> bucket = redissonReactiveClient.getBucket(resourceKey, StringCodec.INSTANCE);
        return bucket.get()
                .onErrorResume(throwable -> Mono.error(new LockInternalError(throwable.getMessage())));
    }
}
