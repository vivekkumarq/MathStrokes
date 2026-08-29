package com.mathstrokes.common.exception;

import java.util.List;

import com.mathstrokes.common.dto.ApiErrorResponse;
import com.mathstrokes.common.dto.FieldErrorItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception into the single ApiErrorResponse envelope.
 * Stack traces are logged, never returned to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex,
                                                             HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), request, ex.getFieldErrors());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError fe
                        ? new FieldErrorItem(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue())
                        : FieldErrorItem.of(error.getObjectName(), error.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, "Validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(v -> FieldErrorItem.of(String.valueOf(v.getPropertyPath()), v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, "Validation failed", request, errors);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_ERROR, "Malformed or missing request data", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex,
                                                                 HttpServletRequest request) {
        return build(ErrorCode.AUTHENTICATION_FAILED, "Invalid credentials", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                               HttpServletRequest request) {
        return build(ErrorCode.ACCESS_DENIED, "You are not allowed to access this resource",
                request, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                                 HttpServletRequest request) {
        return build(ErrorCode.BUSINESS_RULE_VIOLATION,
                "This record was modified by someone else. Reload and try again.", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DUPLICATE_RESOURCE, "The operation conflicts with existing data",
                request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                             HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "No handler for " + request.getRequestURI(),
                request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                     HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED", ex.getMessage(), request.getRequestURI(), List.of());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> build(ErrorCode code, String message,
                                                   HttpServletRequest request,
                                                   List<FieldErrorItem> fieldErrors) {
        ApiErrorResponse body = ApiErrorResponse.of(code.getStatus().value(), code.name(), message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
