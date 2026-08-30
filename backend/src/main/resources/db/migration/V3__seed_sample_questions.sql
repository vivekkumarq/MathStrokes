-- =====================================================================================
-- Sample question bank for development and demonstration.
--
-- Content is LaTeX source, exactly as an admin would type it in the editor; KaTeX
-- renders it in the browser. Nothing here is pre-rendered HTML.
--
-- The arithmetic in these questions was generated from known integer roots rather
-- than typed by hand, so every answer key agrees with its own worked solution.
-- =====================================================================================

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 5x + 6 = 0$$',
           'Factorising, $$x^2 - 5x + 6 = (x - (2))(x - (3)) = 0$$

Hence $x = 2$ or $x = 3$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 2$ or $x = 3$', 0, TRUE),
           ('B', '$x = -2$ or $x = -3$', 1, FALSE),
           ('C', '$x = 3$ or $x = 2$', 2, FALSE),
           ('D', '$x = 1$ or $x = 5$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 7x + 6 = 0$$',
           'Factorising, $$x^2 - 7x + 6 = (x - (1))(x - (6)) = 0$$

Hence $x = 1$ or $x = 6$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 0$ or $x = 8$', 0, FALSE),
           ('B', '$x = 1$ or $x = 6$', 1, TRUE),
           ('C', '$x = -1$ or $x = -6$', 2, FALSE),
           ('D', '$x = 2$ or $x = 5$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'HARD', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 3x - 10 = 0$$',
           'Factorising, $$x^2 - 3x - 10 = (x - (-2))(x - (5)) = 0$$

Hence $x = -2$ or $x = 5$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -1$ or $x = 4$', 0, FALSE),
           ('B', '$x = -3$ or $x = 7$', 1, FALSE),
           ('C', '$x = -2$ or $x = 5$', 2, TRUE),
           ('D', '$x = 2$ or $x = -5$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 + 7x + 12 = 0$$',
           'Factorising, $$x^2 + 7x + 12 = (x - (-3))(x - (-4)) = 0$$

Hence $x = -3$ or $x = -4$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 3$ or $x = 4$', 0, FALSE),
           ('B', '$x = -2$ or $x = -5$', 1, FALSE),
           ('C', '$x = -4$ or $x = -2$', 2, FALSE),
           ('D', '$x = -3$ or $x = -4$', 3, TRUE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 11x + 28 = 0$$',
           'Factorising, $$x^2 - 11x + 28 = (x - (4))(x - (7)) = 0$$

Hence $x = 4$ or $x = 7$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 4$ or $x = 7$', 0, TRUE),
           ('B', '$x = -4$ or $x = -7$', 1, FALSE),
           ('C', '$x = 5$ or $x = 6$', 2, FALSE),
           ('D', '$x = 3$ or $x = 9$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'HARD', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 7x - 8 = 0$$',
           'Factorising, $$x^2 - 7x - 8 = (x - (-1))(x - (8)) = 0$$

Hence $x = -1$ or $x = 8$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -2$ or $x = 10$', 0, FALSE),
           ('B', '$x = -1$ or $x = 8$', 1, TRUE),
           ('C', '$x = 1$ or $x = -8$', 2, FALSE),
           ('D', '$x = 0$ or $x = 7$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 10x + 25 = 0$$',
           'Factorising, $$x^2 - 10x + 25 = (x - (5))(x - (5)) = 0$$

Hence $x = 5$ or $x = 5$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 6$ or $x = 4$', 0, FALSE),
           ('B', '$x = 4$ or $x = 7$', 1, FALSE),
           ('C', '$x = 5$ or $x = 5$', 2, TRUE),
           ('D', '$x = -5$ or $x = -5$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 11x + 18 = 0$$',
           'Factorising, $$x^2 - 11x + 18 = (x - (2))(x - (9)) = 0$$

Hence $x = 2$ or $x = 9$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -2$ or $x = -9$', 0, FALSE),
           ('B', '$x = 3$ or $x = 8$', 1, FALSE),
           ('C', '$x = 1$ or $x = 11$', 2, FALSE),
           ('D', '$x = 2$ or $x = 9$', 3, TRUE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'HARD', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 + 4x - 12 = 0$$',
           'Factorising, $$x^2 + 4x - 12 = (x - (-6))(x - (2)) = 0$$

Hence $x = -6$ or $x = 2$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -6$ or $x = 2$', 0, TRUE),
           ('B', '$x = 6$ or $x = -2$', 1, FALSE),
           ('C', '$x = -5$ or $x = 1$', 2, FALSE),
           ('D', '$x = -7$ or $x = 4$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 13x + 30 = 0$$',
           'Factorising, $$x^2 - 13x + 30 = (x - (3))(x - (10)) = 0$$

Hence $x = 3$ or $x = 10$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 2$ or $x = 12$', 0, FALSE),
           ('B', '$x = 3$ or $x = 10$', 1, TRUE),
           ('C', '$x = -3$ or $x = -10$', 2, FALSE),
           ('D', '$x = 4$ or $x = 9$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 + 7x + 10 = 0$$',
           'Factorising, $$x^2 + 7x + 10 = (x - (-5))(x - (-2)) = 0$$

Hence $x = -5$ or $x = -2$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -4$ or $x = -3$', 0, FALSE),
           ('B', '$x = -6$ or $x = 0$', 1, FALSE),
           ('C', '$x = -5$ or $x = -2$', 2, TRUE),
           ('D', '$x = 5$ or $x = 2$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'HARD', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 13x + 12 = 0$$',
           'Factorising, $$x^2 - 13x + 12 = (x - (1))(x - (12)) = 0$$

Hence $x = 1$ or $x = 12$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -1$ or $x = -12$', 0, FALSE),
           ('B', '$x = 2$ or $x = 11$', 1, FALSE),
           ('C', '$x = 0$ or $x = 14$', 2, FALSE),
           ('D', '$x = 1$ or $x = 12$', 3, TRUE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 9x + 14 = 0$$',
           'Factorising, $$x^2 - 9x + 14 = (x - (7))(x - (2)) = 0$$

Hence $x = 7$ or $x = 2$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 7$ or $x = 2$', 0, TRUE),
           ('B', '$x = -7$ or $x = -2$', 1, FALSE),
           ('C', '$x = 8$ or $x = 1$', 2, FALSE),
           ('D', '$x = 6$ or $x = 4$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 2x - 24 = 0$$',
           'Factorising, $$x^2 - 2x - 24 = (x - (-4))(x - (6)) = 0$$

Hence $x = -4$ or $x = 6$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -5$ or $x = 8$', 0, FALSE),
           ('B', '$x = -4$ or $x = 6$', 1, TRUE),
           ('C', '$x = 4$ or $x = -6$', 2, FALSE),
           ('D', '$x = -3$ or $x = 5$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'HARD', 'SINGLE_CORRECT',
           'Solve for $x$:

$$x^2 - 6x + 9 = 0$$',
           'Factorising, $$x^2 - 6x + 9 = (x - (3))(x - (3)) = 0$$

Hence $x = 3$ or $x = 3$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 4$ or $x = 2$', 0, FALSE),
           ('B', '$x = 2$ or $x = 5$', 1, FALSE),
           ('C', '$x = 3$ or $x = 3$', 2, TRUE),
           ('D', '$x = -3$ or $x = -3$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$2x^2 - 7x + 3 = 0$$

find the sum of the roots.',
           'For $ax^2 + bx + c = 0$ the sum of the roots is $-\frac{b}{a}$.

Here $a = 2$, $b = -7$, $c = 3$, so the sum is $\frac{7}{2}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$-\frac{3}{2}$', 0, FALSE),
           ('B', '$\frac{7}{2}$', 1, TRUE),
           ('C', '$\frac{3}{2}$', 2, FALSE),
           ('D', '$-\frac{7}{2}$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$x^2 + 5x + 6 = 0$$

find the product of the roots.',
           'For $ax^2 + bx + c = 0$ the product of the roots is $\frac{c}{a}$.

Here $a = 1$, $b = 5$, $c = 6$, so the product is $6$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$5$', 0, FALSE),
           ('B', '$-6$', 1, FALSE),
           ('C', '$6$', 2, TRUE),
           ('D', '$-5$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$3x^2 - 2x - 8 = 0$$

find the sum of the roots.',
           'For $ax^2 + bx + c = 0$ the sum of the roots is $-\frac{b}{a}$.

Here $a = 3$, $b = -2$, $c = -8$, so the sum is $\frac{2}{3}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$-\frac{8}{3}$', 0, FALSE),
           ('B', '$-\frac{2}{3}$', 1, FALSE),
           ('C', '$\frac{8}{3}$', 2, FALSE),
           ('D', '$\frac{2}{3}$', 3, TRUE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$2x^2 + 9x + 4 = 0$$

find the product of the roots.',
           'For $ax^2 + bx + c = 0$ the product of the roots is $\frac{c}{a}$.

Here $a = 2$, $b = 9$, $c = 4$, so the product is $2$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$2$', 0, TRUE),
           ('B', '$-\frac{9}{2}$', 1, FALSE),
           ('C', '$\frac{9}{2}$', 2, FALSE),
           ('D', '$-2$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$5x^2 - 3x - 2 = 0$$

find the sum of the roots.',
           'For $ax^2 + bx + c = 0$ the sum of the roots is $-\frac{b}{a}$.

Here $a = 5$, $b = -3$, $c = -2$, so the sum is $\frac{3}{5}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$\frac{2}{5}$', 0, FALSE),
           ('B', '$\frac{3}{5}$', 1, TRUE),
           ('C', '$-\frac{2}{5}$', 2, FALSE),
           ('D', '$-\frac{3}{5}$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$x^2 - 11x + 24 = 0$$

find the product of the roots.',
           'For $ax^2 + bx + c = 0$ the product of the roots is $\frac{c}{a}$.

Here $a = 1$, $b = -11$, $c = 24$, so the product is $24$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$-11$', 0, FALSE),
           ('B', '$-24$', 1, FALSE),
           ('C', '$24$', 2, TRUE),
           ('D', '$11$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$4x^2 + 4x + 1 = 0$$

find the sum of the roots.',
           'For $ax^2 + bx + c = 0$ the sum of the roots is $-\frac{b}{a}$.

Here $a = 4$, $b = 4$, $c = 1$, so the sum is $-1$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$\frac{1}{4}$', 0, FALSE),
           ('B', '$1$', 1, FALSE),
           ('C', '$-\frac{1}{4}$', 2, FALSE),
           ('D', '$-1$', 3, TRUE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$3x^2 + 7x + 2 = 0$$

find the product of the roots.',
           'For $ax^2 + bx + c = 0$ the product of the roots is $\frac{c}{a}$.

Here $a = 3$, $b = 7$, $c = 2$, so the product is $\frac{2}{3}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$\frac{2}{3}$', 0, TRUE),
           ('B', '$-\frac{7}{3}$', 1, FALSE),
           ('C', '$\frac{7}{3}$', 2, FALSE),
           ('D', '$-\frac{2}{3}$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$2x^2 - 5x - 12 = 0$$

find the sum of the roots.',
           'For $ax^2 + bx + c = 0$ the sum of the roots is $-\frac{b}{a}$.

Here $a = 2$, $b = -5$, $c = -12$, so the sum is $\frac{5}{2}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$6$', 0, FALSE),
           ('B', '$\frac{5}{2}$', 1, TRUE),
           ('C', '$-6$', 2, FALSE),
           ('D', '$-\frac{5}{2}$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$6x^2 - 5x + 1 = 0$$

find the product of the roots.',
           'For $ax^2 + bx + c = 0$ the product of the roots is $\frac{c}{a}$.

Here $a = 6$, $b = -5$, $c = 1$, so the product is $\frac{1}{6}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$-\frac{5}{6}$', 0, FALSE),
           ('B', '$-\frac{1}{6}$', 1, FALSE),
           ('C', '$\frac{1}{6}$', 2, TRUE),
           ('D', '$\frac{5}{6}$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$x^2 + 8x + 15 = 0$$

find the sum of the roots.',
           'For $ax^2 + bx + c = 0$ the sum of the roots is $-\frac{b}{a}$.

Here $a = 1$, $b = 8$, $c = 15$, so the sum is $-8$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$15$', 0, FALSE),
           ('B', '$8$', 1, FALSE),
           ('C', '$-15$', 2, FALSE),
           ('D', '$-8$', 3, TRUE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$2x^2 + 3x - 5 = 0$$

find the product of the roots.',
           'For $ax^2 + bx + c = 0$ the product of the roots is $\frac{c}{a}$.

Here $a = 2$, $b = 3$, $c = -5$, so the product is $-\frac{5}{2}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$-\frac{5}{2}$', 0, TRUE),
           ('B', '$-\frac{3}{2}$', 1, FALSE),
           ('C', '$\frac{3}{2}$', 2, FALSE),
           ('D', '$\frac{5}{2}$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$3x^2 - 12x + 9 = 0$$

find the sum of the roots.',
           'For $ax^2 + bx + c = 0$ the sum of the roots is $-\frac{b}{a}$.

Here $a = 3$, $b = -12$, $c = 9$, so the sum is $4$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$-3$', 0, FALSE),
           ('B', '$4$', 1, TRUE),
           ('C', '$3$', 2, FALSE),
           ('D', '$-4$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$5x^2 + 2x - 7 = 0$$

find the product of the roots.',
           'For $ax^2 + bx + c = 0$ the product of the roots is $\frac{c}{a}$.

Here $a = 5$, $b = 2$, $c = -7$, so the product is $-\frac{7}{5}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$\frac{2}{5}$', 0, FALSE),
           ('B', '$\frac{7}{5}$', 1, FALSE),
           ('C', '$-\frac{7}{5}$', 2, TRUE),
           ('D', '$-\frac{2}{5}$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'If $\alpha$ and $\beta$ are the roots of

$$4x^2 - 9x + 2 = 0$$

find the sum of the roots.',
           'For $ax^2 + bx + c = 0$ the sum of the roots is $-\frac{b}{a}$.

Here $a = 4$, $b = -9$, $c = 2$, so the sum is $\frac{9}{4}$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$\frac{1}{2}$', 0, FALSE),
           ('B', '$-\frac{9}{4}$', 1, FALSE),
           ('C', '$-\frac{1}{2}$', 2, FALSE),
           ('D', '$\frac{9}{4}$', 3, TRUE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 7x + 10 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (2))(x - (5)) = 0$, so the roots are $2$ and $5$.

Sum of roots $= 7$, product of roots $= 10$, discriminant $= 9$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 2$ is a root', 0, TRUE),
           ('B', '$x = 5$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $7$', 2, TRUE),
           ('D', 'The product of the roots is $11$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - x - 12 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-3))(x - (4)) = 0$, so the roots are $-3$ and $4$.

Sum of roots $= 1$, product of roots $= -12$, discriminant $= 49$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -3$ is a root', 0, TRUE),
           ('B', '$x = 4$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $1$', 2, TRUE),
           ('D', 'The discriminant is $53$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 8x + 7 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (1))(x - (7)) = 0$, so the roots are $1$ and $7$.

Sum of roots $= 8$, product of roots $= 7$, discriminant $= 36$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 1$ is a root', 0, TRUE),
           ('B', '$x = 7$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $8$', 2, TRUE),
           ('D', '$x = 9$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 + 8x + 12 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-2))(x - (-6)) = 0$, so the roots are $-2$ and $-6$.

Sum of roots $= -8$, product of roots $= 12$, discriminant $= 16$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -2$ is a root', 0, TRUE),
           ('B', '$x = -6$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $-8$', 2, TRUE),
           ('D', 'The product of the roots is $13$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 11x + 24 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (3))(x - (8)) = 0$, so the roots are $3$ and $8$.

Sum of roots $= 11$, product of roots $= 24$, discriminant $= 25$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 3$ is a root', 0, TRUE),
           ('B', '$x = 8$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $11$', 2, TRUE),
           ('D', 'The discriminant is $29$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 8x - 9 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-1))(x - (9)) = 0$, so the roots are $-1$ and $9$.

Sum of roots $= 8$, product of roots $= -9$, discriminant $= 100$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -1$ is a root', 0, TRUE),
           ('B', '$x = 9$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $8$', 2, TRUE),
           ('D', '$x = 9$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 10x + 24 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (4))(x - (6)) = 0$, so the roots are $4$ and $6$.

Sum of roots $= 10$, product of roots $= 24$, discriminant $= 4$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 4$ is a root', 0, TRUE),
           ('B', '$x = 6$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $10$', 2, TRUE),
           ('D', 'The product of the roots is $25$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 + 2x - 15 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-5))(x - (3)) = 0$, so the roots are $-5$ and $3$.

Sum of roots $= -2$, product of roots $= -15$, discriminant $= 64$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -5$ is a root', 0, TRUE),
           ('B', '$x = 3$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $-2$', 2, TRUE),
           ('D', 'The discriminant is $68$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 13x + 22 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (2))(x - (11)) = 0$, so the roots are $2$ and $11$.

Sum of roots $= 13$, product of roots $= 22$, discriminant $= 81$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 2$ is a root', 0, TRUE),
           ('B', '$x = 11$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $13$', 2, TRUE),
           ('D', '$x = 14$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 3x - 28 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-4))(x - (7)) = 0$, so the roots are $-4$ and $7$.

Sum of roots $= 3$, product of roots $= -28$, discriminant $= 121$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -4$ is a root', 0, TRUE),
           ('B', '$x = 7$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $3$', 2, TRUE),
           ('D', 'The product of the roots is $-27$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 7x + 6 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (6))(x - (1)) = 0$, so the roots are $6$ and $1$.

Sum of roots $= 7$, product of roots $= 6$, discriminant $= 25$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 6$ is a root', 0, TRUE),
           ('B', '$x = 1$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $7$', 2, TRUE),
           ('D', 'The discriminant is $29$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 + 5x - 14 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-7))(x - (2)) = 0$, so the roots are $-7$ and $2$.

Sum of roots $= -5$, product of roots $= -14$, discriminant $= 81$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -7$ is a root', 0, TRUE),
           ('B', '$x = 2$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $-5$', 2, TRUE),
           ('D', '$x = -4$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 14x + 45 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (5))(x - (9)) = 0$, so the roots are $5$ and $9$.

Sum of roots $= 14$, product of roots $= 45$, discriminant $= 16$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 5$ is a root', 0, TRUE),
           ('B', '$x = 9$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $14$', 2, TRUE),
           ('D', 'The product of the roots is $46$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 10x - 24 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-2))(x - (12)) = 0$, so the roots are $-2$ and $12$.

Sum of roots $= 10$, product of roots $= -24$, discriminant $= 196$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -2$ is a root', 0, TRUE),
           ('B', '$x = 12$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $10$', 2, TRUE),
           ('D', 'The discriminant is $200$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 11x + 24 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (8))(x - (3)) = 0$, so the roots are $8$ and $3$.

Sum of roots $= 11$, product of roots $= 24$, discriminant $= 25$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 8$ is a root', 0, TRUE),
           ('B', '$x = 3$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $11$', 2, TRUE),
           ('D', '$x = 12$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 11x + 10 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (1))(x - (10)) = 0$, so the roots are $1$ and $10$.

Sum of roots $= 11$, product of roots $= 10$, discriminant $= 81$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 1$ is a root', 0, TRUE),
           ('B', '$x = 10$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $11$', 2, TRUE),
           ('D', 'The product of the roots is $11$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 + x - 30 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-6))(x - (5)) = 0$, so the roots are $-6$ and $5$.

Sum of roots $= -1$, product of roots $= -30$, discriminant $= 121$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -6$ is a root', 0, TRUE),
           ('B', '$x = 5$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $-1$', 2, TRUE),
           ('D', 'The discriminant is $125$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 11x + 28 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (7))(x - (4)) = 0$, so the roots are $7$ and $4$.

Sum of roots $= 11$, product of roots $= 28$, discriminant $= 9$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 7$ is a root', 0, TRUE),
           ('B', '$x = 4$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $11$', 2, TRUE),
           ('D', '$x = 12$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 8x - 33 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-3))(x - (11)) = 0$, so the roots are $-3$ and $11$.

Sum of roots $= 8$, product of roots $= -33$, discriminant $= 196$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -3$ is a root', 0, TRUE),
           ('B', '$x = 11$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $8$', 2, TRUE),
           ('D', 'The product of the roots is $-32$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 11x + 18 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (9))(x - (2)) = 0$, so the roots are $9$ and $2$.

Sum of roots $= 11$, product of roots $= 18$, discriminant $= 49$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 9$ is a root', 0, TRUE),
           ('B', '$x = 2$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $11$', 2, TRUE),
           ('D', 'The discriminant is $53$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 + 7x - 8 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-8))(x - (1)) = 0$, so the roots are $-8$ and $1$.

Sum of roots $= -7$, product of roots $= -8$, discriminant $= 81$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -8$ is a root', 0, TRUE),
           ('B', '$x = 1$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $-7$', 2, TRUE),
           ('D', '$x = -6$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 17x + 52 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (4))(x - (13)) = 0$, so the roots are $4$ and $13$.

Sum of roots $= 17$, product of roots $= 52$, discriminant $= 81$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 4$ is a root', 0, TRUE),
           ('B', '$x = 13$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $17$', 2, TRUE),
           ('D', 'The product of the roots is $53$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 13x - 14 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-1))(x - (14)) = 0$, so the roots are $-1$ and $14$.

Sum of roots $= 13$, product of roots $= -14$, discriminant $= 225$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -1$ is a root', 0, TRUE),
           ('B', '$x = 14$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $13$', 2, TRUE),
           ('D', 'The discriminant is $229$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 13x + 30 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (10))(x - (3)) = 0$, so the roots are $10$ and $3$.

Sum of roots $= 13$, product of roots $= 30$, discriminant $= 49$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 10$ is a root', 0, TRUE),
           ('B', '$x = 3$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $13$', 2, TRUE),
           ('D', '$x = 14$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 3x - 40 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-5))(x - (8)) = 0$, so the roots are $-5$ and $8$.

Sum of roots $= 3$, product of roots $= -40$, discriminant $= 169$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -5$ is a root', 0, TRUE),
           ('B', '$x = 8$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $3$', 2, TRUE),
           ('D', 'The product of the roots is $-39$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 13x + 42 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (6))(x - (7)) = 0$, so the roots are $6$ and $7$.

Sum of roots $= 13$, product of roots $= 42$, discriminant $= 1$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 6$ is a root', 0, TRUE),
           ('B', '$x = 7$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $13$', 2, TRUE),
           ('D', 'The discriminant is $5$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 + 5x - 36 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (-9))(x - (4)) = 0$, so the roots are $-9$ and $4$.

Sum of roots $= -5$, product of roots $= -36$, discriminant $= 169$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = -9$ is a root', 0, TRUE),
           ('B', '$x = 4$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $-5$', 2, TRUE),
           ('D', '$x = -4$ is a root', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'MEDIUM', 'MULTIPLE_CORRECT',
           'Consider the quadratic equation

$$x^2 - 13x + 12 = 0$$

Which of the following statements are correct?',
           'Factorising gives $(x - (12))(x - (1)) = 0$, so the roots are $12$ and $1$.

Sum of roots $= 13$, product of roots $= 12$, discriminant $= 121$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$x = 12$ is a root', 0, TRUE),
           ('B', '$x = 1$ is a root', 1, TRUE),
           ('C', 'The sum of the roots is $13$', 2, TRUE),
           ('D', 'The product of the roots is $13$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'Evaluate the definite integral

$$\int_0^1 x^2 \, dx$$',
           '$$\int_0^1 x^2 \, dx = \left[\frac{x^3}{3}\right]_0^1 = \frac{1}{3} - 0 = \frac{1}{3}$$',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Integrals'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$\frac{1}{2}$', 0, FALSE),
           ('B', '$\frac{1}{3}$', 1, TRUE),
           ('C', '$\frac{1}{4}$', 2, FALSE),
           ('D', '$1$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'Evaluate

$$\int_0^{\pi/2} \sin x \, dx$$',
           '$$\int_0^{\pi/2} \sin x \, dx = \left[-\cos x\right]_0^{\pi/2} = -\cos\frac{\pi}{2} + \cos 0 = 0 + 1 = 1$$',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Integrals'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$0$', 0, FALSE),
           ('B', '$\frac{\pi}{2}$', 1, FALSE),
           ('C', '$1$', 2, TRUE),
           ('D', '$2$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'Evaluate the limit

$$\lim_{x \to 0} \frac{\sin x}{x}$$',
           'This is a standard limit. Expanding $\sin x = x - \frac{x^3}{3!} + \cdots$ gives

$$\frac{\sin x}{x} = 1 - \frac{x^2}{6} + \cdots \to 1 \text{ as } x \to 0$$',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Limits'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$0$', 0, FALSE),
           ('B', '$1$', 1, TRUE),
           ('C', '$\infty$', 2, FALSE),
           ('D', 'The limit does not exist', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'EASY', 'SINGLE_CORRECT',
           'The roots of a quadratic equation $ax^2 + bx + c = 0$ are given by which formula?',
           'Completing the square on $ax^2 + bx + c = 0$ gives the quadratic formula

$$x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}$$',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Quadratic Equations'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}$', 0, TRUE),
           ('B', '$\frac{-b \pm \sqrt{b^2 + 4ac}}{2a}$', 1, FALSE),
           ('C', '$\frac{b \pm \sqrt{b^2 - 4ac}}{2a}$', 2, FALSE),
           ('D', '$\frac{-b \pm \sqrt{4ac - b^2}}{2a}$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'If $z = 3 + 4i$, find $|z|$.',
           '$$|z| = \sqrt{a^2 + b^2} = \sqrt{3^2 + 4^2} = \sqrt{9 + 16} = \sqrt{25} = 5$$',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Complex Numbers'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$3$', 0, FALSE),
           ('B', '$4$', 1, FALSE),
           ('C', '$5$', 2, TRUE),
           ('D', '$7$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'Let $z = 1 + i$. Which of the following are correct?',
           '$|z| = \sqrt{1^2 + 1^2} = \sqrt{2}$ and $\arg z = \frac{\pi}{4}$.

By De Moivre, $z^2 = 2\left(\cos\frac{\pi}{2} + i\sin\frac{\pi}{2}\right) = 2i$.

Hence $z^4 = (2i)^2 = -4$.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Complex Numbers'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$|z| = \sqrt{2}$', 0, TRUE),
           ('B', '$\arg z = \frac{\pi}{4}$', 1, TRUE),
           ('C', '$z^2 = 2i$', 2, TRUE),
           ('D', '$z^4 = 4$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_MAIN', 'MEDIUM', 'SINGLE_CORRECT',
           'Find the determinant of

$$A = \begin{bmatrix} 2 & 3 \ 1 & 4 \end{bmatrix}$$',
           '$$\det A = (2)(4) - (3)(1) = 8 - 3 = 5$$',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Matrices'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$5$', 0, TRUE),
           ('B', '$11$', 1, FALSE),
           ('C', '$-5$', 2, FALSE),
           ('D', '$8$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);

WITH inserted AS (
    INSERT INTO questions (subject_id, chapter_id, exam_pattern, difficulty,
                           question_type, question_content, solution_content,
                           status, published_at)
    SELECT s.id, c.id, 'JEE_ADVANCED', 'HARD', 'MULTIPLE_CORRECT',
           'For a square matrix $A$ of order $n$, which statements are always true?',
           '$\det(A^T) = \det(A)$ and $\det(kA) = k^n \det(A)$ are standard properties. Determinants are multiplicative, so $\det(AB) = \det(A)\det(B)$. However $\det(A + B) \ne \det(A) + \det(B)$ in general.',
           'PUBLISHED', now()
    FROM subjects s
    JOIN chapters c ON c.subject_id = s.id
    WHERE s.code = 'MATH' AND c.name = 'Determinants'
    RETURNING id
)
INSERT INTO question_options (question_id, option_key, content, display_order, is_correct)
SELECT inserted.id, v.option_key, v.content, v.display_order, v.is_correct
FROM inserted,
     (VALUES ('A', '$\det(A^T) = \det(A)$', 0, TRUE),
           ('B', '$\det(kA) = k^n \det(A)$', 1, TRUE),
           ('C', '$\det(AB) = \det(A)\det(B)$', 2, TRUE),
           ('D', '$\det(A + B) = \det(A) + \det(B)$', 3, FALSE)) AS v(option_key, content, display_order, is_correct);
