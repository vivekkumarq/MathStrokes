package com.mathstrokes.user.repository;

import java.time.Instant;

/**
 * Projection for the admin student grid.
 *
 * Exists so the attempt count comes back in the same query as the student row rather than as one
 * extra count per student, which is what a naive listing would cost.
 */
public interface StudentListRow {

    Long getId();

    String getFullName();

    String getPhoneNumber();

    boolean getEnabled();

    Instant getLastLoginAt();

    Instant getRegisteredAt();

    long getAttemptCount();
}
