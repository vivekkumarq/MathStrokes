package com.mathstrokes.exam.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Turns a scheduled instant into the sentence a student reads.
 *
 * Everything is stored and compared in UTC - Hibernate is configured with {@code time_zone: UTC}
 * and the columns are TIMESTAMPTZ - but a student sitting in a classroom in India needs to be told
 * "4:00 PM", not an offset they have to do arithmetic on. So the conversion happens once, here, on
 * the way out.
 *
 * The zone is fixed rather than taken from the request. A class test opens at a wall-clock time
 * the teacher announced to the room, and rendering it in whatever zone a phone happens to be set
 * to would tell two students in the same room two different times. If the platform ever runs a
 * cohort outside India this becomes a property of the test, not of the reader.
 */
public final class TestSchedule {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMMM 'at' h:mm a", Locale.ENGLISH);

    private TestSchedule() {
    }

    /**
     * "4 September at 4:00 PM", or null if there is no such instant. The caller decides what an
     * absent bound means; an unbounded window is not an error and has no sentence to render.
     */
    public static String humanise(Instant instant) {
        return instant == null ? null : WHEN.format(instant.atZone(ZONE));
    }
}
