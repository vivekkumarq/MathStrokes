package com.mathstrokes.common.exception;

/**
 * Thrown when an authenticated principal tries to reach a resource it does not own.
 * Deliberately distinct from Spring Security's role check: this guards object-level access.
 */
public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
}
