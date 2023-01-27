package com.vsware.libraries.redisreactive.lock.ports;

import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public interface LockerPort {
    Mono<Void> lock(String resourceKey, String fencingKey, int ttl, TimeUnit timeUnit);
    Mono<Void> unlock(String resourceKey, String fencingKey);
}
