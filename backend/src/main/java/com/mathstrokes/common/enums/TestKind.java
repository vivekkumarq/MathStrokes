package com.mathstrokes.common.enums;

/**
 * PRACTICE   - the self-service bank a student browses and sits whenever they like.
 * CLASS_TEST - a paper a teacher built by hand for a particular class, usually with a window.
 *
 * Deliberately explicit rather than derived from the presence of a schedule. A teacher may open
 * a class test for the room in front of them with no dates at all, and a derived rule would file
 * that under practice; it would also let a class test rejoin the practice list once its window
 * had passed. The kind is the intent, and the intent does not change when the clock moves.
 */
public enum TestKind {
    PRACTICE,
    CLASS_TEST
}
