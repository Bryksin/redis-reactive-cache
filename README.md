# Redis Reactive Cache for Spring Boot WebFlux

## Description

This Redis Reactive Cache library brings reactive cache functionality to your Spring Boot WebFlux project<br/> 
It is self Auto Configurable, all you need is to import it as dependency.

This library provides 4 annotations:
* `@RedisReactiveCacheAdd` - stores cache after the method execution behind the scenes without blocking server response.
* `@RedisReactiveCacheGet` - gets cache, if cache not available, it will execute the method and store the result (without blocking server response) in cache for future use.
* `@RedisReactiveCacheUpdate` - removes cache without blocking, execute annotated method and store the result in cache (without blocking server response).
* `@RedisReactiveCacheEvict` - removes cache without blocking.

You can annotate your methods with any of them, and it will be automatically cached.
All of those annotations has 2 arguments:
* `key` - cache key, either String or evaluated expressions started with `#` (see in usage examples)
* `useArgsHash` - default is `false`, if you want to use the method arguments hash as cache key postfix,<br/> 
set it to `true`. Very useful for collections parameters. 

## Usage Example:

```java
@Service
public class YourServiceClass {
    
    @RedisReactiveCacheAdd(key = "<your_key>")
    public Mono<TestTable> storeInDb(String name) {
        //your reactive call to DB
        //yourReactiveRepository.save(new DBModel(name));
    }

    @RedisReactiveCacheAdd(key = "names", useArgsHash = true) //CacheKey will be: names_<hash_of_args>
    public Flux<TestTable> storeMultipleInDb(List<String> names) {
        //your reactive call to DB
    }

    @RedisReactiveCacheGet(key = "#name") //CacheKey will be: value of name argument
    public Mono<TestTable> getFromDb(String name) {
        //your reactive call to DB
    }

    @RedisReactiveCacheGet(key = "names", useArgsHash = true)
    public Flux<TestTable> getMultipleFromDb(List<String> names) {
        //your reactive call to DB
    }

    @RedisReactiveCacheUpdate(key = "#testTable.getId().toString()")
    public Mono<TestTable> updateDbRecord(DbModel dbModel) {
        //your reactive call to DB
    }

    @RedisReactiveCacheUpdate(key = "multiple")
    public Flux<TestTable> updateMultipleDbRecords(List<DbModel> dbModels) {
        //your reactive call to DB
    }

    @RedisReactiveCacheEvict(key = "#testTable.getName()")
    public Mono<Void> deleteDbRec(DbModel dbModel) {
        //your reactive call to DB
    }

    @RedisReactiveCacheEvict(key = "names", useArgsHash = true)
    public Mono<Void> deleteMultipleDbRecs(List<DbModel> dbModels) {
        //your reactive call to DB
    }
}
```

These annotations could be used directly on your Reactive Repository interface:
```java
public interface yourReactiveRepo extends ReactiveCrudRepository<YourDBModel, PkType> {

    @RedisReactiveCacheGet(key = "#somefield")
    Mono<DbModel> FindBySomeField(String someField);
}
```

## Properties

Redis connection properties are default Spring Boot properties
```yaml
spring:
  redis:
    host: <your_redis_host>
    port: <your_redis_port>
    #etc...
    
    #Additional properties for this library 
    date_format: "dd-MM-yyyy"
    time_format: "HH:mm:ss"
```
Additionally, you may define your RedisConnectionFactory Bean in the code the way you need it, but not required.

## Build

_**Note:** requires running Docker for TestContainers_

Just run:
```shell
./build-jar.sh
```
or
```shell
./build-jar.sh <optional_build_version> <optional_gradle_action>
```
