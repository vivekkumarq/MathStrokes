package com.mathstrokes.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mathstrokes.common.exception.ApiException;
import com.mathstrokes.common.exception.ErrorCode;
import com.mathstrokes.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Fixed-window throttle for the credential-guessing endpoints: login, the security-answer
 * challenge and recovery initiation.
 *
 * In-process and per-instance by design. That is honest about what it is - it slows down a
 * single attacker against a single node, which is the realistic threat for this deployment, and
 * it needs no Redis. A multi-instance deployment should move this behind a shared store or the
 * load balancer; the interface here would not change.
 */
@Component
public class LoginRateLimiter {

    private final AppProperties appProperties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /** Counts one failed attempt and throws once the key exceeds its allowance. */
    public void recordFailure(String key) {
        AppProperties.RateLimit config = appProperties.getRateLimit();
        if (!config.isEnabled()) {
            return;
        }
        Instant now = Instant.now();
        Window window = windows.compute(normalise(key), (ignored, existing) ->
                existing == null || existing.isExpired(now, config.getWindowSeconds())
                        ? new Window(now)
                        : existing);
        window.count().incrementAndGet();
        pruneOccasionally(now, config.getWindowSeconds());
    }

    /** Rejects the request before any credential work happens if the key is already blocked. */
    public void checkAllowed(String key) {
        AppProperties.RateLimit config = appProperties.getRateLimit();
        if (!config.isEnabled()) {
            return;
        }
        Window window = windows.get(normalise(key));
        if (window == null || window.isExpired(Instant.now(), config.getWindowSeconds())) {
            return;
        }
        if (window.count().get() >= config.getMaxAttempts()) {
            throw new ApiException(ErrorCode.RATE_LIMITED,
                    "Too many attempts. Please wait a few minutes and try again.");
        }
    }

    /** Called after a success so a legitimate user is not punished for earlier typos. */
    public void reset(String key) {
        windows.remove(normalise(key));
    }

    private String normalise(String key) {
        return key == null ? "unknown" : key.trim().toLowerCase();
    }

    private void pruneOccasionally(Instant now, int windowSeconds) {
        if (windows.size() > 10_000) {
            windows.values().removeIf(window -> window.isExpired(now, windowSeconds));
        }
    }

    private record Window(Instant startedAt, AtomicInteger count) {

        Window(Instant startedAt) {
            this(startedAt, new AtomicInteger());
        }

        boolean isExpired(Instant now, int windowSeconds) {
            return startedAt.plus(Duration.ofSeconds(windowSeconds)).isBefore(now);
        }
    }
}
