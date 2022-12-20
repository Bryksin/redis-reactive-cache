# Redis Reactive Cache for Spring Boot WebFlux

## Description

This Redis Reactive Cache library brings reactive cache functionality to your Spring Boot WebFlux project<br/> 
It is self Auto Configurable, all you need is to import it as dependency.

This library provides 4 annotations:
* `@ReactiveCacheAdd` - stores cache after the method execution behind the scenes without blocking server response.
* `@ReactiveCacheGet` - gets cache, if cache not available, it will execute the method and store the result (without blocking server response) in cache for future use.
* `@ReactiveCacheUpdate` - removes cache without blocking, execute annotated method and store the result in cache (without blocking server response).
* `@ReactiveCacheEvict` - removes cache without blocking.
* `@ReactiveCacheFlushAll` - Flush all cache without blocking.

You can annotate your methods with any of them, and it will be automatically cached.
All of those annotations has 2 arguments:
* `key` - cache key, either String or evaluated expressions started with `#` (see in usage examples)
* `useArgsHash` - default is `false`, if you want to use the method arguments hash as cache key postfix,<br/> 
set it to `true`. Very useful for collections parameters. 

## Usage Example:

```java
@Service
public class YourServiceClass {
    
    @ReactiveCacheAdd(key = "<your_key>")
    public Mono<TestTable> storeInDb(String name) {
        //your reactive call to DB
        //yourReactiveRepository.save(new DBModel(name));
    }

    @ReactiveCacheAdd(key = "names", useArgsHash = true) //CacheKey will be: names_<hash_of_args>
    public Flux<TestTable> storeMultipleInDb(List<String> names) {
        //your reactive call to DB
    }

    @ReactiveCacheGet(key = "#name") //CacheKey will be: value of name argument
    public Mono<TestTable> getFromDb(String name) {
        //your reactive call to DB
    }

    @ReactiveCacheGet(key = "names", useArgsHash = true)
    public Flux<TestTable> getMultipleFromDb(List<String> names) {
        //your reactive call to DB
    }

    @ReactiveCacheUpdate(key = "#testTable.getId().toString()")
    public Mono<TestTable> updateDbRecord(DbModel dbModel) {
        //your reactive call to DB
    }

    @ReactiveCacheUpdate(key = "multiple")
    public Flux<TestTable> updateMultipleDbRecords(List<DbModel> dbModels) {
        //your reactive call to DB
    }

    @ReactiveCacheEvict(key = "#testTable.getName()")
    public Mono<Void> deleteDbRec(DbModel dbModel) {
        //your reactive call to DB
    }

    @ReactiveCacheEvict(key = "names", useArgsHash = true)
    public Mono<Void> deleteMultipleDbRecs(List<DbModel> dbModels) {
        //your reactive call to DB
    }

    @ReactiveCacheFlushAll()
    public Mono<Void> CleanUp() {
        //your reactive call to DB
    }
}
```

These annotations could be used directly on your Reactive Repository interface:
```java
public interface yourReactiveRepo extends ReactiveCrudRepository<YourDBModel, PkType> {

    @ReactiveCacheGet(key = "#somefield")
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

## Publish new version
* Create new git tag 
```shell
git tag x.y.z
git push origin x.y.z
```
* In GitHub, create a new released based on the created tag.

## Maven dependency
```xml
<dependencies>
  <dependency>
    <groupId>com.github.Hazem-Ben-Khalfallah</groupId>
    <artifactId>redis-reactive-cache</artifactId>
    <version>x.y.z</version>
  </dependency>
<dependencies>
  
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

## Credits
Special thanks goes to [Bryksin](https://github.com/Bryksin) for starting [this project](https://github.com/Bryksin/redis-reactive-cache) since there is no official alternative for the moment. 