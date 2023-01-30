package com.vsware.libraries.redisreactive.cache.aspect;

import com.vsware.libraries.redisreactive.cache.annotation.ReactiveCacheAdd;
import com.vsware.libraries.redisreactive.cache.errors.UnsupportedReturnTypeError;
import com.vsware.libraries.redisreactive.cache.ports.CachePort;
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

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@ConditionalOnClass({ReactiveRedisTemplate.class})
@RequiredArgsConstructor
public final class ReactiveCacheAddAspect extends AbstractReactiveCacheAspect {

    private final AspectUtils aspectUtils;
    private final CachePort cache;

    /**
     * RedisReactiveCacheAdd - Add result of annotated method to Redis cache
     * Intended to be used on method which creates brand-new record.
     * First returns saved record as server response, and under the hood (without blocking server response) saves record to Redis cache
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactive.cache.annotation.ReactiveCacheAdd)")
    public Object redisReactiveCacheAdd(ProceedingJoinPoint joinPoint) {
        return addToCache(joinPoint,
                getMethod(joinPoint).getReturnType(),
                getKey(joinPoint));
    }

    private String getKey(ProceedingJoinPoint joinPoint) {
        ReactiveCacheAdd annotation = getMethod(joinPoint)
                .getAnnotation(ReactiveCacheAdd.class);
        return aspectUtils.getKeyVal(joinPoint, annotation.key(), annotation.useArgsHash());
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        return aspectUtils.getMethod(joinPoint);
    }

    private CorePublisher<?> addToCache(ProceedingJoinPoint joinPoint, Class<?> returnType, String key) {
        log.info("Add to cache [{}]", key);
        if (returnType.isAssignableFrom(Mono.class)) {
            return putMethodMonoResponseToCache(joinPoint, key);
        } else if (returnType.isAssignableFrom(Flux.class)) {
            return putMethodFluxResponseToCache(joinPoint, key);
        }
        throw new UnsupportedReturnTypeError();
    }

    @Override
    protected CachePort getCache() {
        return cache;
    }
}
