package com.vsware.libraries.redisreactivecache.aspect;

import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheEvict;
import com.vsware.libraries.redisreactivecache.ports.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@ConditionalOnClass({ReactiveRedisTemplate.class})
@RequiredArgsConstructor
public class RedisReactiveCacheEvictAspect extends AbstractRedisReactiveCacheAddAspect {

    private final AspectUtils aspectUtils;
    private final CachePort cache;

    /**
     * RedisReactiveCacheEvict - Delete cache from Redis and delete stored record
     * Intended to be used on method which update some records in DB.
     * Evict cache from Redis without waiting for response, in the main time execute annotated method
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheEvict)")
    public Object redisReactiveCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        return evictCache(joinPoint,
                getKey(joinPoint));
    }

    private String getKey(ProceedingJoinPoint joinPoint) {
        RedisReactiveCacheEvict annotation = aspectUtils.getMethod(joinPoint)
                .getAnnotation(RedisReactiveCacheEvict.class);
        return aspectUtils.getKeyVal(joinPoint, annotation.key(), annotation.useArgsHash());
    }

    private Object evictCache(ProceedingJoinPoint joinPoint, String key) throws Throwable {
        log.info("Evaluated Redis cacheKey: " + key);
        cache.delete(key).subscribe();
        return joinPoint.proceed(joinPoint.getArgs());
    }

    @Override
    CachePort getCache() {
        return cache;
    }
}
