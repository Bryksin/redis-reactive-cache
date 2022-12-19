package com.vsware.libraries.redisreactivecache.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReactiveCacheEvict {
    String key() default "";
    boolean useArgsHash() default false;
}
