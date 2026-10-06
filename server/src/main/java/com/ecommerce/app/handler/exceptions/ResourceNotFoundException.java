package com.ecommerce.app.handler.exceptions;

/**
 * Thrown when a resource does not exist or belongs to another user. Both cases map to 404 so
 * the API never reveals whether someone else's resource exists.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " not found");
    }
}
