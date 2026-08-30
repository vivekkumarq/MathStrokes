package com.mathstrokes.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Best-effort client address, for throttling only.
 *
 * Behind a platform proxy (Render, Fly, a load balancer) the socket address is the proxy's, so
 * the leftmost entry of X-Forwarded-For is the closest thing to the caller. That header is
 * client-supplied and trivially spoofed, which is exactly why this is used ONLY to rate limit
 * and never for authentication or authorisation: the worst a forged value can do is let an
 * attacker spread their own requests across buckets, which is the situation we are already in
 * without the header.
 */
public final class ClientIpResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
