package com.mathstrokes.attempt.dto;

import java.time.Instant;

/**
 * The server's view of the clock, returned with every attempt-bearing response.
 *
 * All three fields matter. {@code remainingSeconds} is the server's own arithmetic and is
 * authoritative; {@code serverTime} and {@code expiresAt} let the client measure its offset from
 * the server and keep ticking accurately between calls instead of trusting the local clock.
 */
public record AttemptTimingResponse(Instant serverTime, Instant startedAt, Instant expiresAt,
                                    long remainingSeconds, boolean expired) {
}
