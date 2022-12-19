package com.vsware.libraries.redisreactivecache.service;

import com.github.javafaker.Faker;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheAdd;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheEvict;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheFlushAll;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheGet;
import com.vsware.libraries.redisreactivecache.annotation.RedisReactiveCacheUpdate;
import com.vsware.libraries.redisreactivecache.model.TestTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TestService {

    public final static AtomicInteger methodCall = new AtomicInteger();
    @Autowired
    private Faker faker;

    @RedisReactiveCacheAdd(key = "#name")
    public Mono<TestTable> storeInDb(String name) throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Mono.just(new TestTable(1, name, LocalDateTime.now()));
        //end
    }

    @RedisReactiveCacheAdd(key = "names", useArgsHash = true)
    public Flux<TestTable> storeMultipleInDb(List<String> names) throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Flux.fromIterable(
                IntStream.range(0, names.size())
                        .mapToObj(index -> new TestTable(index, names.get(index), LocalDateTime.now()))
                        .collect(Collectors.toList())
        );
        //end
    }

    @RedisReactiveCacheGet(key = "#name")
    public Mono<TestTable> getFromDb(String name) throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Mono.just(new TestTable(1, name, LocalDateTime.now()));
        //end
    }

    @RedisReactiveCacheGet(key = "names", useArgsHash = true)
    public Flux<TestTable> getMultipleFromDb(List<String> names) throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Flux.fromIterable(
                IntStream.range(0, names.size())
                        .mapToObj(index -> new TestTable(index, names.get(index), LocalDateTime.now()))
                        .collect(Collectors.toList())
        );
        //end
    }

    @RedisReactiveCacheUpdate(key = "#testTable.getId().toString()")
    public Mono<TestTable> updateDbRecord(TestTable testTable) throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Mono.just(testTable);
        //end
    }

    @RedisReactiveCacheUpdate(key = "multiple")
    public Flux<TestTable> updateMultipleDbRecords(List<TestTable> testTables) throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Flux.fromIterable(testTables);
        //end
    }

    @RedisReactiveCacheEvict(key = "#testTable.getName()")
    public Mono<Void> deleteDbRec(TestTable testTable) throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Mono.empty();
        //end
    }

    @RedisReactiveCacheEvict(key = "names", useArgsHash = true)
    public Mono<Void> deleteMultipleDbRecs(List<TestTable> testTables) throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Mono.empty();
        //end
    }

    @RedisReactiveCacheFlushAll()
    public Mono<Void> flushAllDbRecs() throws InterruptedException {
        //imitating call to db
        Thread.sleep(10);
        methodCall.incrementAndGet();
        return Mono.empty();
        //end
    }
}
