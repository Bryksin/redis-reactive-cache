package com.vsware.libraries.redisreactive.cache.aspect;

import com.vsware.libraries.redisreactive.cache.ports.CachePort;
import org.aspectj.lang.ProceedingJoinPoint;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hazem
 */
abstract class AbstractReactiveCacheAspect {

    abstract CachePort getCache();

    protected Mono<?> putMethodMonoResponseToCache(ProceedingJoinPoint joinPoint, String key) {
        return proceedAsMono(joinPoint)
                .map(methodResponse -> {
                    getCache().set(key, methodResponse).subscribe();
                    return methodResponse;
                });
    }

    protected Flux<?> putMethodFluxResponseToCache(ProceedingJoinPoint joinPoint, String key) {
        return proceedAsFlux(joinPoint)
                .collectList()
                .map(methodResponseList -> {
                    getCache().set(key, methodResponseList).subscribe();
                    return methodResponseList;
                })
                .flatMapMany(Flux::fromIterable);
    }

    protected Mono<?> proceedAsMono(ProceedingJoinPoint joinPoint) {
        try {
            return ((Mono<?>) proceedMethod(joinPoint));
        } catch (Throwable e) {
            return Mono.error(e);
        }
    }

    protected Flux<?> proceedAsFlux(ProceedingJoinPoint joinPoint) {
        try {
            return ((Flux<?>) proceedMethod(joinPoint));
        } catch (Throwable e) {
            return Flux.error(e);
        }
    }

    protected Object proceedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed(joinPoint.getArgs());
    }
}
