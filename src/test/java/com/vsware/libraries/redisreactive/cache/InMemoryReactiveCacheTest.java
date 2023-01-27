package com.vsware.libraries.redisreactive.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.vsware.libraries.redisreactive.cache.model.TestTable;
import com.vsware.libraries.redisreactive.cache.service.TestService;
import com.vsware.libraries.redisreactive.cache.adapters.InMemoryCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@SpringBootTest
@ActiveProfiles(profiles = "in-memory-test")
class InMemoryReactiveCacheTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    @Autowired
    private TestService testService;
    @Autowired
    private InMemoryCache inMemoryCache;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Faker faker;

    private String calculateCacheKey(String key, Object anyArg) {
        return key + "_" + Arrays.hashCode(new Object[]{anyArg});
    }

    @AfterEach
    void cleanRedis() {
        testService.methodCall.set(0);
        inMemoryCache.reset();
    }

    @Test
    void test_getFromDb_whenCacheFails() throws InterruptedException {
        // given
        inMemoryCache.forceError();

        // when
        testService.getFromDb(faker.name().firstName()).block();

        // then
        checkOriginalMethodIsExecutedNTimes(1);
    }

    @Test
    void test_storeInDb_whenCacheFails() throws InterruptedException {
        // given
        inMemoryCache.forceError();

        // when
        testService.storeInDb(faker.name().firstName()).block();

        // then
        checkOriginalMethodIsExecutedNTimes(1);
    }

    @Test
    void test_updateDbRecord_whenCacheFails() throws InterruptedException {
        // given
        inMemoryCache.forceError();

        // when
        testService.updateDbRecord(new TestTable(1, faker.name().firstName(), LocalDateTime.now())).block();

        // then
        checkOriginalMethodIsExecutedNTimes(1);
    }

    @Test
    void test_deleteDbRec_whenCacheFails() throws InterruptedException {
        // given
        inMemoryCache.forceError();

        // when
        testService.deleteDbRec(new TestTable(1, faker.name().firstName(), LocalDateTime.now())).block();

        // then
        checkOriginalMethodIsExecutedNTimes(1);
    }

    @Test
    void test_flushAllDbRecs_whenCacheFails() throws InterruptedException {
        // given
        inMemoryCache.forceError();

        // when
        testService.flushAllDbRecs().block();

        // then
        checkOriginalMethodIsExecutedNTimes(1);
    }

    private void checkOriginalMethodIsExecutedNTimes(int count) {
        assert testService.methodCall.get() == count;
    }

}
