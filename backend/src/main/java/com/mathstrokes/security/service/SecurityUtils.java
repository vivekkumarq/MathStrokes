package com.mathstrokes.security.service;

import java.util.Optional;

import com.mathstrokes.common.exception.ApiException;
import com.mathstrokes.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The only sanctioned way to learn who is calling. Controllers and services must never accept a
 * user id from the request body or path when deciding ownership.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static UserPrincipal requirePrincipal() {
        return currentPrincipal().orElseThrow(
                () -> new ApiException(ErrorCode.AUTHENTICATION_FAILED, "Not authenticated"));
    }

    public static Long requireUserId() {
        return requirePrincipal().id();
    }
}
