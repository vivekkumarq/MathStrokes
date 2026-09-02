/**
 * Mirrors com.mathstrokes.common.enums. These are transported as plain strings, so they
 * are modelled as string-literal unions rather than TS enums: no runtime cost, and an
 * unknown value from the server is a compile error at the point of use rather than a
 * silent numeric mismatch.
 *
 * Keep in lockstep with the backend. When BE publishes /v3/api-docs these become
 * generated; until then this file is the contract.
 */

export const ROLE_NAMES = ['ROLE_ADMIN', 'ROLE_STUDENT'] as const;
export type RoleName = (typeof ROLE_NAMES)[number];

export const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'] as const;
export type Difficulty = (typeof DIFFICULTIES)[number];

/**
 * Deliberately separate from Difficulty: a JEE Advanced question can be EASY and a
 * JEE Main question can be HARD.
 */
export const EXAM_PATTERNS = ['JEE_MAIN', 'JEE_ADVANCED'] as const;
export type ExamPattern = (typeof EXAM_PATTERNS)[number];

export const QUESTION_TYPES = ['SINGLE_CORRECT', 'MULTIPLE_CORRECT'] as const;
export type QuestionType = (typeof QUESTION_TYPES)[number];

export const QUESTION_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED'] as const;
export type QuestionStatus = (typeof QUESTION_STATUSES)[number];

export const TEST_STATUSES = ['DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED'] as const;
export type TestStatus = (typeof TEST_STATUSES)[number];

export const TEST_GENERATION_MODES = ['FIXED_SET', 'RANDOM_PER_ATTEMPT'] as const;
export type TestGenerationMode = (typeof TEST_GENERATION_MODES)[number];

/**
 * What a paper is FOR. A practice test is always-on self-study; a class test is one the
 * teacher schedules, hand-picks the questions for, and flags live for a specific sitting.
 *
 * Explicit rather than derived from the presence of a schedule: a teacher who flags a paper
 * live right now, with no date on it, still means it as a class test, and deriving the kind
 * from the window would silently file that paper under practice.
 */
export const TEST_KINDS = ['PRACTICE', 'CLASS_TEST'] as const;
export type TestKind = (typeof TEST_KINDS)[number];

export const QUESTION_RESULT_STATUSES = [
  'CORRECT',
  'PARTIALLY_CORRECT',
  'INCORRECT',
  'UNANSWERED',
] as const;
export type QuestionResultStatus = (typeof QUESTION_RESULT_STATUSES)[number];

/** Palette state for a single question inside an attempt. Server-persisted so it survives a refresh. */
export const ANSWER_STATUSES = [
  'NOT_VISITED',
  'NOT_ANSWERED',
  'ANSWERED',
  'MARKED_FOR_REVIEW',
  'ANSWERED_AND_MARKED_FOR_REVIEW',
] as const;
export type AnswerStatus = (typeof ANSWER_STATUSES)[number];

export const ATTEMPT_STATUSES = [
  'NOT_STARTED',
  'ACTIVE',
  'SUBMITTED',
  'AUTO_SUBMITTED',
  'EVALUATED',
] as const;
export type AttemptStatus = (typeof ATTEMPT_STATUSES)[number];

// --- Helpers mirroring the behaviour the backend enums carry -------------------------
// The server is authoritative for all of these; these exist so the UI can render
// optimistically between requests without inventing its own rules.

export function answerStatusOf(
  visited: boolean,
  answered: boolean,
  markedForReview: boolean,
): AnswerStatus {
  if (!visited && !answered && !markedForReview) {
    return 'NOT_VISITED';
  }
  if (answered) {
    return markedForReview ? 'ANSWERED_AND_MARKED_FOR_REVIEW' : 'ANSWERED';
  }
  return markedForReview ? 'MARKED_FOR_REVIEW' : 'NOT_ANSWERED';
}

export function isAnswered(status: AnswerStatus): boolean {
  return status === 'ANSWERED' || status === 'ANSWERED_AND_MARKED_FOR_REVIEW';
}

export function isMarkedForReview(status: AnswerStatus): boolean {
  return status === 'MARKED_FOR_REVIEW' || status === 'ANSWERED_AND_MARKED_FOR_REVIEW';
}

const FINALISED_ATTEMPT_STATUSES: readonly AttemptStatus[] = [
  'SUBMITTED',
  'AUTO_SUBMITTED',
  'EVALUATED',
];

/** True once the attempt can no longer accept answers. The runner must go read-only. */
export function isAttemptFinalised(status: AttemptStatus): boolean {
  return FINALISED_ATTEMPT_STATUSES.includes(status);
}

export function canAttemptTransitionTo(from: AttemptStatus, to: AttemptStatus): boolean {
  switch (from) {
    case 'NOT_STARTED':
      return to === 'ACTIVE';
    case 'ACTIVE':
      return to === 'SUBMITTED' || to === 'AUTO_SUBMITTED';
    case 'SUBMITTED':
    case 'AUTO_SUBMITTED':
      return to === 'EVALUATED';
    case 'EVALUATED':
      return false;
  }
}
