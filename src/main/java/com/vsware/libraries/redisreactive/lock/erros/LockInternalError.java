package com.vsware.libraries.redisreactive.lock.erros;

/**
 * @author Bilel
 */
public class LockInternalError extends RuntimeException {

    public LockInternalError(String message) {
        super(message);
    }
}
