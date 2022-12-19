package com.vsware.libraries.redisreactivecache.errors;


/**
 * @author hazem
 */
public class UnsupportedReturnTypeError extends RuntimeException {

    public UnsupportedReturnTypeError() {
        super("Annotated method has unsupported return type. Expected Mono<?> or Flux<?>");
    }
}
