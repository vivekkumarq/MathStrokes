-- =====================================================================================
-- One published test per chapter per exam pattern: 28 x 2 = 56 papers.
--
-- A question bank is not a test. V4 filled every chapter with questions, but a student can
-- only sit a paper that an admin has published, so without this the app has 1,521 questions
-- and two things to actually do.
--
-- Every test is FIXED_SET: the 25 questions are drawn once, here, and pinned. That is what
-- makes the cohort comparable and is the only mode under which ranking is enabled - two
-- students who answered different questions cannot be placed on one leaderboard.
--
-- max_attempts_per_student is 5 rather than 1. These are demonstration papers meant to be
-- re-sat while exploring the marking schemes; a real examination would use 1.
-- =====================================================================================

DO $seedtests$
DECLARE
    subject_id_v   bigint;
    chapter_row    record;
    pattern_v      text;
    test_id_v      bigint;
    available      integer;
    created        integer := 0;
    skipped        integer := 0;
    title_v        text;
BEGIN
    SELECT id INTO subject_id_v FROM subjects WHERE code = 'MATH';
    IF subject_id_v IS NULL THEN
        RAISE EXCEPTION 'Mathematics subject is missing';
    END IF;

    FOR chapter_row IN
        SELECT id, name FROM chapters WHERE subject_id = subject_id_v ORDER BY display_order, name
    LOOP
        FOREACH pattern_v IN ARRAY ARRAY['JEE_MAIN', 'JEE_ADVANCED'] LOOP

            title_v := chapter_row.name || ' - '
                       || CASE pattern_v WHEN 'JEE_MAIN' THEN 'JEE Main' ELSE 'JEE Advanced' END
                       || ' - Chapter Test';

            -- Idempotent: re-running must not produce a second copy of the same paper.
            IF EXISTS (SELECT 1 FROM tests WHERE title = title_v) THEN
                skipped := skipped + 1;
                CONTINUE;
            END IF;

            -- A paper short of its 25 questions would mis-score and corrupt the cohort, so
            -- skip the chapter rather than publish a broken test.
            SELECT count(*) INTO available
            FROM questions
            WHERE chapter_id = chapter_row.id
              AND exam_pattern = pattern_v
              AND status = 'PUBLISHED';

            IF available < 25 THEN
                RAISE NOTICE 'Skipping % - only % published question(s)', title_v, available;
                skipped := skipped + 1;
                CONTINUE;
            END IF;

            INSERT INTO tests (title, description, subject_id, chapter_id, exam_pattern,
                               duration_minutes, question_count, generation_mode,
                               status, ranking_enabled, max_attempts_per_student, published_at)
            VALUES (title_v,
                    CASE pattern_v
                        WHEN 'JEE_MAIN' THEN
                            'Twenty-five single-correct questions on ' || chapter_row.name
                            || '. JEE Main marking: +4 correct, -1 incorrect, 0 unattempted.'
                        ELSE
                            'Twenty-five multiple-correct questions on ' || chapter_row.name
                            || '. JEE Advanced partial marking: +4 for the exact key, +1 per '
                            || 'correct option up to +3, -2 for any wrong selection.'
                    END,
                    subject_id_v, chapter_row.id, pattern_v,
                    60, 25, 'FIXED_SET', 'PUBLISHED', TRUE, 5, now())
            RETURNING id INTO test_id_v;

            -- Pin the paper now. Drawn at random from the published pool, then fixed forever.
            INSERT INTO test_questions (test_id, question_id, question_order)
            SELECT test_id_v, picked.id, picked.ord
            FROM (
                SELECT q.id, row_number() OVER (ORDER BY random()) AS ord
                FROM questions q
                WHERE q.chapter_id = chapter_row.id
                  AND q.exam_pattern = pattern_v
                  AND q.status = 'PUBLISHED'
                LIMIT 25
            ) AS picked;

            created := created + 1;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Published % chapter test(s), skipped %', created, skipped;
END
$seedtests$;

-- The two original demonstration tests were capped at a single attempt, which leaves anyone
-- exploring the platform locked out after one sitting. Same reasoning as above.
UPDATE tests SET max_attempts_per_student = 5 WHERE max_attempts_per_student = 1;
