package com.mathstrokes.common.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Allowed transitions:
 *   NOT_STARTED -> ACTIVE
 *   ACTIVE      -> SUBMITTED | AUTO_SUBMITTED
 *   SUBMITTED   -> EVALUATED
 *   AUTO_SUBMITTED -> EVALUATED
 *   EVALUATED   -> (terminal)
 */
public enum AttemptStatus {
    NOT_STARTED,
    ACTIVE,
    SUBMITTED,
    AUTO_SUBMITTED,
    EVALUATED;

    private static final Set<AttemptStatus> FINALISED =
            EnumSet.of(SUBMITTED, AUTO_SUBMITTED, EVALUATED);

    public boolean isFinalised() {
        return FINALISED.contains(this);
    }

    public boolean canTransitionTo(AttemptStatus target) {
        return switch (this) {
            case NOT_STARTED -> target == ACTIVE;
            case ACTIVE -> target == SUBMITTED || target == AUTO_SUBMITTED;
            case SUBMITTED, AUTO_SUBMITTED -> target == EVALUATED;
            case EVALUATED -> false;
        };
    }
}
