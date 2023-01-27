package com.vsware.libraries.redisreactive.cache.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReactiveCacheUpdate {
    String key() default "";
    boolean useArgsHash() default false;
}
