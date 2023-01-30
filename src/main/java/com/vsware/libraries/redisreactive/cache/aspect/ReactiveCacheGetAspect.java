package com.vsware.libraries.redisreactive.cache.aspect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsware.libraries.redisreactive.cache.annotation.ReactiveCacheGet;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@ConditionalOnClass({ReactiveRedisTemplate.class})
@RequiredArgsConstructor
public final class ReactiveCacheGetAspect extends AbstractReactiveCacheAspect {

    private final AspectUtils aspectUtils;
    private final ObjectMapper objectMapper;
    private final CachePort cache;

    /**
     * RedisReactiveCacheGet - Read cache from Redis by key
     * Intended to be used on method which returns some records from DB.
     * First read Redis Cache, if result is empty, read DB, return response back to user and under the hood (without blocking server response)
     * set Redis with missing cache - to be available for next request.
     * If Redis cache exists - return cache, don't query DB
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactive.cache.annotation.ReactiveCacheGet)")
    public Object redisReactiveCacheGet(ProceedingJoinPoint joinPoint) {
        Method method = getMethod(joinPoint);
        return getFromCacheIfAvailable(joinPoint,
                method.getReturnType(),
                getKey(joinPoint),
                aspectUtils.getTypeReference(method));
    }

    private String getKey(ProceedingJoinPoint joinPoint) {
        ReactiveCacheGet annotation = getMethod(joinPoint)
                .getAnnotation(ReactiveCacheGet.class);
        return aspectUtils.getKeyVal(joinPoint, annotation.key(), annotation.useArgsHash());
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        return aspectUtils.getMethod(joinPoint);
    }

    private CorePublisher<Object> getFromCacheIfAvailable(ProceedingJoinPoint joinPoint,
                                                          Class<?> returnType,
                                                          String key,
                                                          TypeReference<Object> typeRefForMapper) {
        log.info("Get from cache [{}]", key);
        if (returnType.isAssignableFrom(Mono.class)) {
            return cache.get(key)
                    .map(cacheResponse -> objectMapper.convertValue(cacheResponse, typeRefForMapper))
                    .switchIfEmpty(Mono.defer(() -> putMethodMonoResponseToCache(joinPoint, key)))
                    .onErrorResume(throwable -> proceedAsMono(joinPoint))
                    ;
        }

        if (returnType.isAssignableFrom(Flux.class)) {
            return cache.get(key)
                    .flatMapMany(cacheResponse -> convertToFlux(typeRefForMapper, (List<Object>) cacheResponse))
                    .switchIfEmpty(Flux.defer(() -> putMethodFluxResponseToCache(joinPoint, key)))
                    .onErrorResume(throwable -> proceedAsFlux(joinPoint))
                    ;
        }
        throw new UnsupportedReturnTypeError();
    }

    private Flux<Object> convertToFlux(TypeReference<Object> typeRefForMapper, List<Object> cacheResponse) {
        return Flux.fromIterable(cacheResponse.stream()
                .map(elem -> objectMapper.convertValue(elem, typeRefForMapper))
                .collect(Collectors.toList()));
    }

    @Override
    protected CachePort getCache() {
        return cache;
    }
}
