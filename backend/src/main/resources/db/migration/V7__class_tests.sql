-- =====================================================================================
-- Class tests: a paper a teacher builds by hand for a specific class, on a specific date.
--
-- Everything about the paper itself already existed. A FIXED_SET test materialises its
-- questions into test_questions at publish time, and V1's zero-guard in TestService.publish
-- means a set attached BEFORE publishing survives untouched. So hand-picking is not a new
-- kind of paper; it is the existing fixed set, written by a teacher instead of drawn by the
-- selector. This migration adds only what genuinely could not be expressed.
--
-- Two columns, one of which breaks a rule established in V6.
--
-- V6 chose absence-as-signal over a scope flag: chapter_id NULL means full syllabus, so no
-- second column can ever disagree with it. The obvious reading of that precedent here is to
-- derive "class test" from "has a scheduled window", adding no flag at all. That reading is
-- wrong, for two reasons:
--
--   1. A teacher who opens a test for the class standing in front of them right now sets no
--      date. The derived rule files that paper under practice, which is precisely backwards.
--   2. A derived rule is not stable over time. Once the window passed, a class test would
--      silently rejoin the practice list, and the same paper would change category on a
--      Tuesday because of what the clock said.
--
-- Absence is a good signal when the absent thing is the definition. Here the window is a
-- consequence of the intent, not the intent, so the intent gets a column. It is an enum
-- rather than a boolean for the same reason generation_mode is one: the third case arrives
-- eventually and a boolean cannot hold it.
--
-- The window does NOT publish anything. Publishing stays a deliberate act by the teacher and
-- the window is a second, independent gate evaluated when a student asks to start. There is
-- no scheduled job here on purpose: this backend runs on a free tier that suspends after
-- ~15 minutes idle, and a scheduler that misses its wake-up would fail to open a test with a
-- class already sitting in front of it. A switch a person flips cannot fail that way.
-- =====================================================================================

ALTER TABLE tests
    ADD COLUMN test_kind          VARCHAR(20) NOT NULL DEFAULT 'PRACTICE',
    ADD COLUMN scheduled_start_at TIMESTAMPTZ,
    ADD COLUMN scheduled_end_at   TIMESTAMPTZ;

-- Defaulting to PRACTICE is what makes this migration a no-op for the 58 papers already in
-- the bank: every one of them keeps exactly the meaning it had, with nothing to backfill.
ALTER TABLE tests
    ADD CONSTRAINT ck_tests_test_kind CHECK (test_kind IN ('PRACTICE', 'CLASS_TEST'));

-- Either bound may stand alone. A start with no end is a paper that opens and stays open; an
-- end with no start is one available immediately that stops being so. Only the ordering is
-- nonsense, so only the ordering is refused.
ALTER TABLE tests
    ADD CONSTRAINT ck_tests_schedule_order CHECK (
        scheduled_start_at IS NULL
        OR scheduled_end_at IS NULL
        OR scheduled_end_at > scheduled_start_at);

COMMENT ON COLUMN tests.test_kind IS
    'PRACTICE for the self-service bank a student browses; CLASS_TEST for a paper a teacher '
    'built by hand for a class. Explicit rather than derived from the schedule: a teacher may '
    'open a class test immediately with no window at all.';

COMMENT ON COLUMN tests.scheduled_start_at IS
    'Students may not START before this instant. NULL means no lower bound. Does not publish '
    'the test and does not end an attempt already in flight.';

COMMENT ON COLUMN tests.scheduled_end_at IS
    'Students may not START after this instant. NULL means no upper bound. An attempt already '
    'running finishes on its own clock, matching how closing a test behaves.';

-- Students browsing class tests filter on kind, then order by when the paper opens. The
-- existing idx_tests_browse leads with status and chapter_id and cannot serve that.
CREATE INDEX idx_tests_kind_schedule ON tests (test_kind, status, scheduled_start_at);
