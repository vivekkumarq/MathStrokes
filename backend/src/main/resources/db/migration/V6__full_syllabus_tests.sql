-- =====================================================================================
-- Full-syllabus papers: questions drawn from every chapter rather than one.
--
-- A test was previously defined as belonging to exactly one chapter, so "draw from the whole
-- syllabus" could not be expressed at all. Rather than add a scope column that could disagree
-- with chapter_id, chapter_id simply becomes nullable and its ABSENCE means full syllabus.
-- One source of truth, no migration of existing rows, and every current test keeps working
-- because every current test has a chapter.
--
-- A correction that cannot go where it belongs: V5's header says the bank holds 1,521 questions.
-- The real figure for a freshly migrated database is 1,522 - 66 from V3 plus 1,456 from V4. V5 is
-- deliberately left wrong, because editing an applied migration changes its checksum and Flyway
-- then refuses to start against any database that already ran the old text. A comment is not
-- worth a failed deploy. Corrections go in the next migration, never in a previous one.
-- =====================================================================================

ALTER TABLE tests ALTER COLUMN chapter_id DROP NOT NULL;

COMMENT ON COLUMN tests.chapter_id IS
    'The chapter this test draws from. NULL means the full syllabus - questions are drawn from '
    'every chapter of the subject for the given exam pattern.';

-- The browse index assumed a chapter was always present. Rebuilt so full-syllabus tests are
-- still found by a student filtering on pattern alone.
DROP INDEX IF EXISTS idx_tests_browse;
CREATE INDEX idx_tests_browse ON tests (status, exam_pattern, chapter_id);

DO $fullsyllabus$
DECLARE
    subject_id_v bigint;
    pattern_v    text;
    title_v      text;
    test_id_v    bigint;
    available    integer;
    created      integer := 0;
BEGIN
    SELECT id INTO subject_id_v FROM subjects WHERE code = 'MATH';
    IF subject_id_v IS NULL THEN
        RAISE EXCEPTION 'Mathematics subject is missing';
    END IF;

    FOREACH pattern_v IN ARRAY ARRAY['JEE_MAIN', 'JEE_ADVANCED'] LOOP
        title_v := 'Full Syllabus - '
                   || CASE pattern_v WHEN 'JEE_MAIN' THEN 'JEE Main' ELSE 'JEE Advanced' END;

        IF EXISTS (SELECT 1 FROM tests WHERE title = title_v) THEN
            CONTINUE;
        END IF;

        SELECT count(*) INTO available
        FROM questions
        WHERE subject_id = subject_id_v AND exam_pattern = pattern_v AND status = 'PUBLISHED';

        IF available < 25 THEN
            RAISE NOTICE 'Skipping % - only % published question(s)', title_v, available;
            CONTINUE;
        END IF;

        INSERT INTO tests (title, description, subject_id, chapter_id, exam_pattern,
                           duration_minutes, question_count, generation_mode,
                           status, ranking_enabled, max_attempts_per_student, published_at)
        VALUES (title_v,
                CASE pattern_v
                    WHEN 'JEE_MAIN' THEN
                        'Twenty-five single-correct questions drawn from across the whole '
                        || 'Mathematics syllabus. JEE Main marking: +4 correct, -1 incorrect, '
                        || '0 unattempted.'
                    ELSE
                        'Twenty-five multiple-correct questions drawn from across the whole '
                        || 'Mathematics syllabus. JEE Advanced partial marking: +4 for the exact '
                        || 'key, +1 per correct option up to +3, -2 for any wrong selection.'
                END,
                subject_id_v,
                NULL,               -- the whole point: no chapter
                pattern_v,
                60, 25, 'FIXED_SET', 'PUBLISHED', TRUE, 5, now())
        RETURNING id INTO test_id_v;

        -- Spread the draw across chapters rather than taking 25 at random, which on a bank this
        -- shape would cluster: one question from each of 25 different chapters makes the paper
        -- genuinely full-syllabus instead of nominally so.
        INSERT INTO test_questions (test_id, question_id, question_order)
        SELECT test_id_v, picked.id, picked.ord
        FROM (
            SELECT id, row_number() OVER (ORDER BY random()) AS ord
            FROM (
                SELECT DISTINCT ON (q.chapter_id) q.id, q.chapter_id
                FROM questions q
                WHERE q.subject_id = subject_id_v
                  AND q.exam_pattern = pattern_v
                  AND q.status = 'PUBLISHED'
                ORDER BY q.chapter_id, random()
            ) one_per_chapter
            LIMIT 25
        ) AS picked;

        created := created + 1;
    END LOOP;

    RAISE NOTICE 'Published % full-syllabus test(s)', created;
END
$fullsyllabus$;
