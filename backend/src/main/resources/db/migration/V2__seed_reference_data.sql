-- =====================================================================================
-- Reference data. Idempotent: safe to re-run, and safe on a database that already has rows.
-- Account and question-bank seeding lives in the Java bootstrap seeder instead, because the
-- admin password has to be BCrypt-hashed at runtime rather than baked into a migration.
-- =====================================================================================

INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN',   'Teacher and platform administrator'),
    ('ROLE_STUDENT', 'Examination candidate')
ON CONFLICT (name) DO NOTHING;

INSERT INTO subjects (name, code, description, active, display_order) VALUES
    ('Mathematics', 'MATH', 'Mathematics for JEE Main and JEE Advanced', TRUE, 1)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chapters (subject_id, name, description, active, display_order)
SELECT s.id, c.name, c.description, TRUE, c.display_order
FROM subjects s
CROSS JOIN (VALUES
    ('Relations and Functions',        'Types of relations, functions, composition and inverse', 1),
    ('Complex Numbers',                'Argand plane, modulus, argument, De Moivre theorem',      2),
    ('Quadratic Equations',            'Roots, discriminant, nature of roots, common roots',      3),
    ('Sequences and Series',           'AP, GP, HP, AGP and summation techniques',                4),
    ('Permutations and Combinations',  'Counting principles, arrangements and selections',        5),
    ('Binomial Theorem',               'General term, middle term, binomial coefficients',        6),
    ('Matrices',                       'Operations, transpose, inverse, elementary operations',   7),
    ('Determinants',                   'Properties, minors, cofactors, Cramer rule',              8),
    ('Limits',                         'Evaluation, standard limits, indeterminate forms',        9),
    ('Continuity and Differentiability', 'Continuity, differentiability, mean value theorems',   10),
    ('Applications of Derivatives',    'Tangents, normals, monotonicity, maxima and minima',     11),
    ('Integrals',                      'Indefinite and definite integrals, properties',          12),
    ('Differential Equations',         'Order, degree, variable separable, linear equations',    13),
    ('Vectors',                        'Dot and cross product, scalar triple product',           14),
    ('Three Dimensional Geometry',     'Lines, planes, shortest distance, angles',               15),
    ('Probability',                    'Conditional probability, Bayes theorem, distributions',  16),
    ('Statistics',                     'Mean, median, mode, variance and standard deviation',    17)
) AS c(name, description, display_order)
WHERE s.code = 'MATH'
ON CONFLICT (subject_id, name) DO NOTHING;

-- -------------------------------------------------------------------- marking schemes
-- JEE Main single correct: +4 / -1 / 0.
INSERT INTO marking_schemes (name, description, exam_pattern, question_type, configuration, active)
VALUES (
    'JEE Main - Single Correct',
    'Standard JEE Main objective marking: +4 correct, -1 incorrect, 0 unattempted.',
    'JEE_MAIN', 'SINGLE_CORRECT',
    '{"fullCorrectMarks": 4.00, "wrongMarks": -1.00, "unansweredMarks": 0.00,
      "partialCreditMode": "NONE", "marksPerCorrectOption": 0.00,
      "maxPartialMarks": 0.00, "noPartialCreditMarks": 0.00}'::jsonb,
    TRUE)
ON CONFLICT (name) DO NOTHING;

-- JEE Main multiple correct: no partial credit, any deviation from the exact key is wrong.
INSERT INTO marking_schemes (name, description, exam_pattern, question_type, configuration, active)
VALUES (
    'JEE Main - Multiple Correct',
    'Exact-match multiple correct: +4 for the exact key, -1 otherwise, 0 unattempted.',
    'JEE_MAIN', 'MULTIPLE_CORRECT',
    '{"fullCorrectMarks": 4.00, "wrongMarks": -1.00, "unansweredMarks": 0.00,
      "partialCreditMode": "NONE", "marksPerCorrectOption": 0.00,
      "maxPartialMarks": 0.00, "noPartialCreditMarks": 0.00}'::jsonb,
    TRUE)
ON CONFLICT (name) DO NOTHING;

-- JEE Advanced single correct: +3 / -1 / 0.
INSERT INTO marking_schemes (name, description, exam_pattern, question_type, configuration, active)
VALUES (
    'JEE Advanced - Single Correct',
    'JEE Advanced single correct: +3 correct, -1 incorrect, 0 unattempted.',
    'JEE_ADVANCED', 'SINGLE_CORRECT',
    '{"fullCorrectMarks": 3.00, "wrongMarks": -1.00, "unansweredMarks": 0.00,
      "partialCreditMode": "NONE", "marksPerCorrectOption": 0.00,
      "maxPartialMarks": 0.00, "noPartialCreditMarks": 0.00}'::jsonb,
    TRUE)
ON CONFLICT (name) DO NOTHING;

-- JEE Advanced multiple correct, partial marking as used from 2023 onwards:
--   all correct options and nothing else            -> +4
--   only correct options but not all of them        -> +1 per selected option, capped at +3
--   any incorrect option selected                   -> -2
--   nothing selected                                ->  0
INSERT INTO marking_schemes (name, description, exam_pattern, question_type, configuration, active)
VALUES (
    'JEE Advanced - Multiple Correct (Partial)',
    'Partial marking: +4 exact, +1 per correct option capped at +3, -2 on any wrong selection.',
    'JEE_ADVANCED', 'MULTIPLE_CORRECT',
    '{"fullCorrectMarks": 4.00, "wrongMarks": -2.00, "unansweredMarks": 0.00,
      "partialCreditMode": "PER_CORRECT_OPTION", "marksPerCorrectOption": 1.00,
      "maxPartialMarks": 3.00, "noPartialCreditMarks": 0.00}'::jsonb,
    TRUE)
ON CONFLICT (name) DO NOTHING;
