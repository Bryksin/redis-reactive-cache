package com.vsware.libraries.redisreactivecache.aspect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheAdd;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheEvict;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheGet;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheUpdate;
import com.vsware.libraries.redisreactivecache.util.AspectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
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
public class RedisReactiveCacheAspect {

    private final ReactiveRedisTemplate reactiveRedisTemplate;
    private final AspectUtils aspectUtils;
    private final ObjectMapper objectMapper;
    /*
    RedisReactiveCacheAdd - Add result of annotated method to Redis cache
    Intended to be used on method which creates brand new record
    Example: ReactiveCrudRepository.save(brandNewRecord) or ReactiveCrudRepository.saveAll(brandNewRecordList)

    First returns saved record as server response, and under the hood (without blocking server response) saves record to Redis cache
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheAdd)")
    public Object redisReactiveCacheAdd(ProceedingJoinPoint joinPoint) {
        Method method = aspectUtils.getMethod(joinPoint);
        Class<?> returnType = method.getReturnType();
        RedisReactiveCacheAdd annotation = method.getAnnotation(RedisReactiveCacheAdd.class);
        String key = aspectUtils.getKeyVal(joinPoint, annotation.key(), annotation.useArgsHash());
        log.info("Evaluated Redis cacheKey: " + key);
        if (returnType.isAssignableFrom(Mono.class)) {
            return methodMonoResponseToCache(joinPoint, key);
        } else if (returnType.isAssignableFrom(Flux.class)) {
            return methodFluxResponseToCache(joinPoint, key);
        }
        throw new RuntimeException("RedisReactiveCacheAdd: Annotated method has unsupported return type, expected Mono<?> or Flux<?>");
    }

    /*
    RedisReactiveCacheGet - Read cache from Redis by key
    Intended to be used on method which returns some records from DB
    Example: ReactiveCrudRepository.findBy...(params) or ReactiveCrudRepository.findAllBy...(params)

    First read Redis Cache, if result is empty, read DB, return response back to user and under the hood (without blocking server response)
    set Redis with missing cache - to be available for next request.
    If Redis cache exists - return cache, don't query DB
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheGet)")
    public Object redisReactiveCacheGet(ProceedingJoinPoint joinPoint) {
        Method method = aspectUtils.getMethod(joinPoint);
        Class<?> rawReturnType = method.getReturnType();
        RedisReactiveCacheGet annotation = method.getAnnotation(RedisReactiveCacheGet.class);
        String key = aspectUtils.getKeyVal(joinPoint, annotation.key(), annotation.useArgsHash());
        TypeReference typeRefForMapper = aspectUtils.getTypeReference(method);
        log.info("Evaluated Redis cacheKey: " + key);
        if (rawReturnType.isAssignableFrom(Mono.class)) {
            return reactiveRedisTemplate.opsForValue().get(key).map(cacheResponse ->
                            objectMapper.convertValue(cacheResponse, typeRefForMapper))
                    .switchIfEmpty(Mono.defer(() -> methodMonoResponseToCache(joinPoint, key)));
        } else if (rawReturnType.isAssignableFrom(Flux.class)) {
            return reactiveRedisTemplate.opsForValue().get(key)
                    .flatMapMany(cacheResponse -> Flux.fromIterable(
                            (List) ((List) cacheResponse).stream()
                                    .map(elem -> objectMapper.convertValue(elem, typeRefForMapper))
                                    .collect(Collectors.toList())))
                    .switchIfEmpty(Flux.defer(() -> methodFluxResponseToCache(joinPoint, key)));
        }
        throw new RuntimeException("RedisReactiveCacheGet: Annotated method has unsupported return type, expected Mono<?> or Flux<?>");
    }

    /*
    RedisReactiveCacheUpdate - Delete cache from Redis and update it with new stored record
    Intended to be used on method which update some records in DB
    Example: ReactiveCrudRepository.save(updatedNewRecord) or ReactiveCrudRepository.saveAll(updatedNewRecordList)

    Evict cache from Redis without waiting for response, in the main time sore updated record in DB,
    then return updated record as server response, and under the hood (without blocking server response)
    saves updated record to Redis cache
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheUpdate)")
    public Object redisReactiveCacheUpdate(ProceedingJoinPoint joinPoint) {
        Method method = aspectUtils.getMethod(joinPoint);
        Class<?> returnType = method.getReturnType();
        RedisReactiveCacheUpdate annotation = method.getAnnotation(RedisReactiveCacheUpdate.class);
        String key = aspectUtils.getKeyVal(joinPoint, annotation.key(), annotation.useArgsHash());
        log.info("Evaluated Redis cacheKey: " + key);
        if (returnType.isAssignableFrom(Mono.class)) {
            reactiveRedisTemplate.opsForValue().delete(key).subscribe();
            return methodMonoResponseToCache(joinPoint, key);
        } else if (returnType.isAssignableFrom(Flux.class)) {
            reactiveRedisTemplate.opsForValue().delete(key).subscribe();
            return methodFluxResponseToCache(joinPoint, key);
        }
        throw new RuntimeException("RedisReactiveCacheUpdate: Annotated method has unsupported return type, expected Mono<?> or Flux<?>");
    }


    /*
    RedisReactiveCacheEvict - Delete cache from Redis and delete stored record
    Intended to be used on method which update some records in DB
    Example: ReactiveCrudRepository.delete(recordToDelete) or ReactiveCrudRepository.deleteAll(recordToDeleteList)

    Evict cache from Redis without waiting for response, in the main time execute annotated method
     */
    @Around("execution(public * *(..)) && @annotation(com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheEvict)")
    public Object redisReactiveCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = aspectUtils.getMethod(joinPoint);
        Class<?> returnType = method.getReturnType();
        RedisReactiveCacheEvict annotation = method.getAnnotation(RedisReactiveCacheEvict.class);
        String key = aspectUtils.getKeyVal(joinPoint, annotation.key(), annotation.useArgsHash());
        log.info("Evaluated Redis cacheKey: " + key);
        reactiveRedisTemplate.opsForValue().delete(key).subscribe();
        return joinPoint.proceed(joinPoint.getArgs());
    }


    private Mono<?> methodMonoResponseToCache(ProceedingJoinPoint joinPoint, String key) {
        try {
            return ((Mono<?>) joinPoint.proceed(joinPoint.getArgs())).map(methodResponse -> {
                reactiveRedisTemplate.opsForValue().set(key, methodResponse).subscribe();
                return methodResponse;
            });
        } catch (Throwable e) {
            return Mono.error(e);
        }
    }

    private Flux<?> methodFluxResponseToCache(ProceedingJoinPoint joinPoint, String key) {
        try {
            return ((Flux<?>) joinPoint.proceed(joinPoint.getArgs())).collectList().map(methodResponseList -> {
                reactiveRedisTemplate.opsForValue().set(key, methodResponseList).subscribe();
                return methodResponseList;
            }).flatMapMany(Flux::fromIterable);
        } catch (Throwable e) {
            return Flux.error(e);
        }
    }
}
