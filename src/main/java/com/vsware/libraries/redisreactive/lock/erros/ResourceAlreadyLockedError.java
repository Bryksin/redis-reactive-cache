package com.vsware.libraries.redisreactive.lock.erros;

/**
 * @author Bilel
 */
public class ResourceAlreadyLockedError extends RuntimeException {

    public ResourceAlreadyLockedError() {
        super("Resource is already locked.");
    }
}
