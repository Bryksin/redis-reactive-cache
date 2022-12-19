package com.vsware.libraries.redisreactivecache.aspect;

import com.vsware.libraries.redisreactivecache.ports.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@ConditionalOnClass({ReactiveRedisTemplate.class})
@RequiredArgsConstructor
public final class RedisReactiveCacheFlushAllAspect extends AbstractRedisReactiveCacheAddAspect{

    private final CachePort cache;

    /**
     * RedisReactiveCacheFlushAll - Delete all cache enties from Redis
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheFlushAll)")
    public Object redisReactiveCacheFlushAll(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Flush all keys from cache");
        cache.flushAll().subscribe();
        return joinPoint.proceed(joinPoint.getArgs());
    }

    @Override
    protected CachePort getCache() {
        return cache;
    }
}
