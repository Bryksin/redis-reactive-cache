package com.vsware.libraries.redisreactivecache.aspect;

import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheUpdate;
import com.vsware.libraries.redisreactivecache.errors.UnsupportedReturnTypeError;
import com.vsware.libraries.redisreactivecache.ports.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@ConditionalOnClass({ReactiveRedisTemplate.class})
@RequiredArgsConstructor
public final class RedisReactiveCacheUpdateAspect extends AbstractRedisReactiveCacheAddAspect {

    private final AspectUtils aspectUtils;
    private final CachePort cache;

    /**
     * RedisReactiveCacheUpdate - Delete cache from Redis and update it with new stored record
     * Intended to be used on method which update some records in DB
     * Evict cache from Redis without waiting for response, in the main time sore updated record in DB,
     * then return updated record as server response, and under the hood (without blocking server response)
     * saves updated record to Redis cache
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheUpdate)")
    public Object redisReactiveCacheUpdate(ProceedingJoinPoint joinPoint) {
        return updateCache(joinPoint,
                getMethod(joinPoint).getReturnType(),
                getKey(joinPoint));
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        return aspectUtils.getMethod(joinPoint);
    }

    private String getKey(ProceedingJoinPoint joinPoint) {
        RedisReactiveCacheUpdate annotation = getMethod(joinPoint)
                .getAnnotation(RedisReactiveCacheUpdate.class);
        return aspectUtils.getKeyVal(joinPoint, annotation.key(), annotation.useArgsHash());
    }

    private CorePublisher<?> updateCache(ProceedingJoinPoint joinPoint, Class<?> returnType, String key) {
        log.info("Evaluated Redis cacheKey: " + key);
        if (returnType.isAssignableFrom(Mono.class)) {
            cache.delete(key).subscribe();
            return putMethodMonoResponseToCache(joinPoint, key);
        } else if (returnType.isAssignableFrom(Flux.class)) {
            cache.delete(key).subscribe();
            return putMethodFluxResponseToCache(joinPoint, key);
        }

        throw new UnsupportedReturnTypeError();
    }

    @Override
    protected CachePort getCache() {
        return cache;
    }
}
