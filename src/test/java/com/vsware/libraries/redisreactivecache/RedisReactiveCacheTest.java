package com.vsware.libraries.redisreactivecache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.vsware.libraries.redisreactivecache.config.RedisTestContainerConfig;
import com.vsware.libraries.redisreactivecache.model.TestTable;
import com.vsware.libraries.redisreactivecache.service.TestService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest
class RedisReactiveCacheTest {

    @Autowired
    private TestService testService;
    @Autowired
    private ReactiveRedisTemplate reactiveRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Faker faker;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private String calculateCacheKey(String key, Object anyArg) {
        return key + "_" + Arrays.hashCode(new Object[]{anyArg});
    }

    @BeforeEach
    void verifyRedisIsEmpty() {
        StepVerifier.create(reactiveRedisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().dbSize())
                .expectNext(0L)
                .verifyComplete();
    }

    @AfterEach
    void cleanRedis() {
        reactiveRedisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().subscribe();
        testService.methodCall.set(0);
    }

    @AfterAll
    static void stopTestContainer() {
        RedisTestContainerConfig.redisContainer.stop();
    }


    @Test
    void test_storeInDb() throws InterruptedException {
        String name = faker.name().firstName();
        TestTable testTable = testService.storeInDb(name).block();

        //Verify now Redis contains cache
        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(name).log())
                .expectNextMatches(redisResp -> {
                    try {
                        TestTable cacheResponse = objectMapper.convertValue(redisResp, TestTable.class);
                        return Objects.equals(cacheResponse.getId(), testTable.getId()) &&
                                cacheResponse.getName().equals(testTable.getName()) &&
                                cacheResponse.getInsertDate().format(formatter).equals(testTable.getInsertDate().format(formatter));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_storeMultipleInDb() throws InterruptedException {

        List<String> names = IntStream.range(0, 10).mapToObj(index -> faker.name().firstName()).collect(Collectors.toList());

        List<TestTable> testTables = testService.storeMultipleInDb(names).collectList().block();
        String cacheKey = calculateCacheKey("names", names);

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(cacheKey).log())
                .expectNextMatches(redisResp -> {
                    try {
                        List<TestTable> cacheResponse = objectMapper.convertValue(redisResp, new TypeReference<List<TestTable>>(){});
                        return cacheResponse.size() == testTables.size() &&
                                cacheResponse.get(5).getName().equals(testTables.get(5).getName());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_getFromDb_whenCacheDoesntExists() throws InterruptedException {
        String name = faker.name().firstName();
        TestTable testTable = testService.getFromDb(name).block();

        //Verify now Redis contains cache
        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(name).log())
                .expectNextMatches(redisResp -> {
                    try {
                        TestTable cacheResponse = objectMapper.convertValue(redisResp, TestTable.class);
                        return Objects.equals(cacheResponse.getId(), testTable.getId()) &&
                                cacheResponse.getName().equals(testTable.getName()) &&
                                cacheResponse.getInsertDate().format(formatter).equals(testTable.getInsertDate().format(formatter));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_getFromDb_whenCacheExists() throws InterruptedException {
        String name = faker.name().firstName();
        TestTable valForCache = new TestTable(1, name, LocalDateTime.now());
        //Create cache to exist
        reactiveRedisTemplate.opsForValue().set(name, valForCache).block();
        //call method
        TestTable testTable = testService.getFromDb(name).block();

        //Verify now Redis contains cache
        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(name).log())
                .expectNextMatches(redisResp -> {
                    try {
                        TestTable cacheResponse = objectMapper.convertValue(redisResp, TestTable.class);
                        return Objects.equals(cacheResponse.getId(), testTable.getId()) &&
                                cacheResponse.getName().equals(testTable.getName()) &&
                                cacheResponse.getInsertDate().format(formatter).equals(testTable.getInsertDate().format(formatter));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        //make sure actual method with potential DB call was not executed
        assert testService.methodCall.get() == 0;
    }

    @Test
    void test_getMultipleFromDb_whenCacheExists() throws InterruptedException {

        List<String> names = IntStream.range(0, 10).mapToObj(index -> faker.name().firstName()).collect(Collectors.toList());
        String cacheKey = calculateCacheKey("names", names);
        List<TestTable> valForCache = IntStream.range(0, names.size())
                .mapToObj(index -> new TestTable(index, names.get(index), LocalDateTime.now()))
                .collect(Collectors.toList());
        //Create cache to exist
        reactiveRedisTemplate.opsForValue().set(cacheKey, valForCache).block();

        List<TestTable> testTables = testService.getMultipleFromDb(names).collectList().block();

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(cacheKey).log())
                .expectNextMatches(redisResp -> {
                    try {
                        List<TestTable> cacheResponse = objectMapper.convertValue(redisResp, new TypeReference<List<TestTable>>(){});
                        return cacheResponse.size() == testTables.size() &&
                                cacheResponse.get(5).getName().equals(testTables.get(5).getName());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        assert testService.methodCall.get() == 0;
    }

    @Test
    void test_getMultipleFromDb_whenCacheDoesntExists() throws InterruptedException {

        List<String> names = IntStream.range(0, 10).mapToObj(index -> faker.name().firstName()).collect(Collectors.toList());

        List<TestTable> testTables = testService.getMultipleFromDb(names).collectList().block();
        String cacheKey = calculateCacheKey("names", names);

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(cacheKey).log())
                .expectNextMatches(redisResp -> {
                    try {
                        List<TestTable> cacheResponse = objectMapper.convertValue(redisResp, new TypeReference<List<TestTable>>(){});
                        return cacheResponse.size() == testTables.size() &&
                                cacheResponse.get(5).getName().equals(testTables.get(5).getName());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_updateDbRecord_whenCacheExists() throws InterruptedException {
        TestTable oldCache = new TestTable(1, faker.name().firstName(), LocalDateTime.now());
        //Create cache to exist
        reactiveRedisTemplate.opsForValue().set("1", oldCache).block();

        //call method
        TestTable testTable = testService.updateDbRecord(new TestTable(1, faker.name().firstName(), LocalDateTime.now())).block();

        //Verify now Redis contains updated cache
        StepVerifier.create(reactiveRedisTemplate.opsForValue().get("1").log())
                .expectNextMatches(redisResp -> {
                    try {
                        TestTable cacheResponse = objectMapper.convertValue(redisResp, TestTable.class);
                        return Objects.equals(cacheResponse.getId(), testTable.getId()) &&
                                cacheResponse.getName().equals(testTable.getName()) &&
                                cacheResponse.getInsertDate().format(formatter).equals(testTable.getInsertDate().format(formatter));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        //make sure actual method with potential DB call was not executed
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_updateDbRecord_whenCacheDoesntExists() throws InterruptedException {
        //call method
        TestTable testTable = testService.updateDbRecord(new TestTable(1, faker.name().firstName(), LocalDateTime.now())).block();

        //Verify now Redis contains updated cache
        StepVerifier.create(reactiveRedisTemplate.opsForValue().get("1").log())
                .expectNextMatches(redisResp -> {
                    try {
                        TestTable cacheResponse = objectMapper.convertValue(redisResp, TestTable.class);
                        return Objects.equals(cacheResponse.getId(), testTable.getId()) &&
                                cacheResponse.getName().equals(testTable.getName()) &&
                                cacheResponse.getInsertDate().format(formatter).equals(testTable.getInsertDate().format(formatter));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        //make sure actual method with potential DB call was not executed
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_updateMultipleDbRecords_whenCacheExists() throws InterruptedException {

        List<String> names = IntStream.range(0, 10).mapToObj(index -> faker.name().firstName()).collect(Collectors.toList());
        String cacheKey = "multiple";
        List<TestTable> valForCache = IntStream.range(0, names.size())
                .mapToObj(index -> new TestTable(index, names.get(index), LocalDateTime.now()))
                .collect(Collectors.toList());
        //Create cache to exist
        reactiveRedisTemplate.opsForValue().set(cacheKey, valForCache).block();

        //modify values
        List<TestTable> recsToUpdate = valForCache.stream().peek(item -> item.setName(faker.name().firstName())).collect(Collectors.toList());
        List<TestTable> testTables = testService.updateMultipleDbRecords(recsToUpdate).collectList().block();

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(cacheKey).log())
                .expectNextMatches(redisResp -> {

                    try {
                        List<TestTable> cacheResponse = objectMapper.convertValue(redisResp, new TypeReference<List<TestTable>>(){});
                        return cacheResponse.size() == testTables.size() &&
                                cacheResponse.get(5).getName().equals(testTables.get(5).getName());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_updateMultipleDbRecords_whenCacheDoesntExists() throws InterruptedException {
        String cacheKey = "multiple";

        List<TestTable> testTables = testService.updateMultipleDbRecords(
                IntStream.range(0, 10)
                        .mapToObj(index -> new TestTable(index, faker.name().firstName(), LocalDateTime.now()))
                        .collect(Collectors.toList()))
                .collectList().block();

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(cacheKey).log())
                .expectNextMatches(redisResp -> {

                    try {
                        List<TestTable> cacheResponse = objectMapper.convertValue(redisResp, new TypeReference<List<TestTable>>(){});
                        return cacheResponse.size() == testTables.size() &&
                                cacheResponse.get(5).getName().equals(testTables.get(5).getName());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_deleteDbRec_whenCacheExists() throws InterruptedException {

        String name = faker.name().firstName();
        TestTable valForCache = new TestTable(1, name, LocalDateTime.now());
        //Create cache to exist
        reactiveRedisTemplate.opsForValue().set(name, valForCache).block();

        //Deleting
        testService.deleteDbRec(valForCache).block();

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(name).log())
                .expectNextCount(0)
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_deleteDbRec_whenCacheDoesntExists() throws InterruptedException {
        String name = faker.name().firstName();
        //Deleting
        testService.deleteDbRec(new TestTable(1, name, LocalDateTime.now())).block();

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(name).log())
                .expectNextCount(0)
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_deleteMultipleDbRecs_whenCacheExists() throws InterruptedException {

        List<TestTable> testTables = IntStream.range(0, 10)
                .mapToObj(index -> new TestTable(index, faker.name().firstName(), LocalDateTime.now()))
                .collect(Collectors.toList());
        String cacheKey = calculateCacheKey("names", testTables);

        //Create cache to exist
        reactiveRedisTemplate.opsForValue().set(cacheKey, testTables).block();

        //Deleting
        testService.deleteMultipleDbRecs(testTables).block();

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(cacheKey).log())
                .expectNextCount(0)
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

    @Test
    void test_deleteMultipleDbRecs_whenCacheDoesntExists() throws InterruptedException {

        List<TestTable> testTables = IntStream.range(0, 10)
                .mapToObj(index -> new TestTable(index, faker.name().firstName(), LocalDateTime.now()))
                .collect(Collectors.toList());
        String cacheKey = calculateCacheKey("names", testTables);

        //Deleting
        testService.deleteMultipleDbRecs(testTables).block();

        StepVerifier.create(reactiveRedisTemplate.opsForValue().get(cacheKey).log())
                .expectNextCount(0)
                .verifyComplete();
        assert testService.methodCall.get() == 1;
    }

}
