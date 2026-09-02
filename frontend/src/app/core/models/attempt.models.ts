import {
  AnswerStatus,
  AttemptStatus,
  Difficulty,
  ExamPattern,
  QuestionResultStatus,
  QuestionType,
  TestKind,
} from './enums';

/**
 * The exam engine contract.
 *
 * Conditionally-present fields are `field?: T` because the API omits nulls rather than
 * sending them. On AttemptResultResponse this matters a lot: every scoring field is
 * absent until the attempt has actually been evaluated.
 */

/**
 * The server's clock, returned on start, on resume, and on every autosave ack.
 *
 * `remainingSeconds` is the server's own arithmetic and is authoritative. serverTime and
 * expiresAt exist so the client can hold an offset and keep its local tick honest between
 * calls — never to compute remaining time from the browser clock alone.
 */
export interface AttemptTiming {
  serverTime: string;
  startedAt: string;
  expiresAt: string;
  remainingSeconds: number;
  expired: boolean;
}

/**
 * An option as shown DURING a live attempt.
 *
 * There is deliberately no `isCorrect` here — the field does not exist in this payload,
 * verified both in the live response and in the OpenAPI schema. The key lives only on
 * ReviewOptionResponse.
 */
export interface AttemptOption {
  id: number;
  optionKey: string;
  content: string;
  displayOrder: number;
}

export interface AttemptQuestion {
  attemptQuestionId: number;
  questionOrder: number;
  /**
   * Drives radio vs checkbox. Read THIS, never the paper's examPattern: a JEE Advanced
   * paper may contain single-correct questions.
   */
  questionType: QuestionType;
  difficulty: Difficulty;
  /** LaTeX. Rendered with KaTeX. */
  questionContent: string;
  options: AttemptOption[];
  selectedOptionIds: number[];
  answerStatus: AnswerStatus;
  markedForReview: boolean;
  visited: boolean;
}

export interface PaletteEntry {
  attemptQuestionId: number;
  questionOrder: number;
  answerStatus: AnswerStatus;
}

/**
 * The whole paper in one response, from POST /attempts (start AND resume) and
 * GET /attempts/active. A reload repaints entirely from this — there is no per-question
 * fetch, so the student never waits between questions.
 */
export interface AttemptResponse {
  attemptId: number;
  testId: number;
  testTitle: string;
  subjectName: string;
  /**
   * Absent for a full-syllabus paper AND for a cross-chapter class test. Optional because
   * the server omits it, which the previous required typing quietly contradicted.
   */
  chapterName?: string;
  /**
   * Always present. Name the paper from this FIRST and fall back to chapterName only for a
   * PRACTICE test: "Full syllabus" is the right label for a practice paper drawn across
   * every chapter, and a lie about a three-question class test that simply spans a few.
   */
  testKind: TestKind;
  examPattern: ExamPattern;
  status: AttemptStatus;
  durationMinutes: number;
  totalQuestions: number;
  /** Server's current sequence for this attempt; seed the local counter from it on resume. */
  clientSequence: number;
  timing: AttemptTiming;
  questions: AttemptQuestion[];
}

export interface StartAttemptRequest {
  testId: number;
}

/**
 * Autosave. Always sends the COMPLETE selection, never a delta — an empty array clears
 * the answer. That is what makes a retry safe.
 */
export interface SaveAnswerRequest {
  attemptQuestionId: number;
  selectedOptionIds: number[];
  markedForReview: boolean;
  visited: boolean;
  /**
   * Monotonic per-attempt counter. A save arriving with a lower sequence than the stored
   * one is discarded server-side and the ack returns the CURRENT state with
   * accepted: false — so a late retry can never clobber a newer answer.
   */
  clientSequence: number;
}

export interface SaveAnswerResponse {
  attemptQuestionId: number;
  selectedOptionIds: number[];
  answerStatus: AnswerStatus;
  markedForReview: boolean;
  clientSequence: number;
  /** False when this write lost to a newer one. Reconcile the UI from this response. */
  accepted: boolean;
  /** All entries, refreshed — the navigator updates from this ack, with no second call. */
  palette: PaletteEntry[];
  timing: AttemptTiming;
}

/**
 * Result of a finished attempt.
 *
 * Everything from `score` down is ABSENT until the attempt is evaluated, so this type is
 * honest about that rather than pretending zeroes. Guard on `status === 'EVALUATED'`.
 *
 * Rank is a SNAPSHOT: it legitimately changes as other students finish the same paper,
 * while the score never moves. Re-fetch on focus rather than trusting a cached rank.
 */
export interface AttemptResultResponse {
  attemptId: number;
  testId: number;
  testTitle: string;
  subjectName: string;
  /**
   * Absent for a full-syllabus paper AND for a cross-chapter class test. Optional because
   * the server omits it, which the previous required typing quietly contradicted.
   */
  chapterName?: string;
  /**
   * Always present. Name the paper from this FIRST and fall back to chapterName only for a
   * PRACTICE test: "Full syllabus" is the right label for a practice paper drawn across
   * every chapter, and a lie about a three-question class test that simply spans a few.
   */
  testKind: TestKind;
  examPattern: ExamPattern;
  status: AttemptStatus;
  startedAt: string;
  submittedAt?: string;
  timeTakenSeconds?: number;
  durationMinutes: number;
  totalQuestions: number;
  rankingEnabled: boolean;

  score?: number;
  maxScore?: number;
  negativeMarks?: number;
  correctCount?: number;
  partiallyCorrectCount?: number;
  incorrectCount?: number;
  unansweredCount?: number;
  attemptedCount?: number;
  accuracy?: number;
  attemptRate?: number;

  rankPosition?: number;
  totalCandidates?: number;
  percentile?: number;
}

/** Option as shown in POST-SUBMISSION review. This is the only payload carrying the key. */
export interface ReviewOption extends AttemptOption {
  isCorrect: boolean;
  selected: boolean;
}

export interface ReviewQuestion {
  attemptQuestionId: number;
  questionOrder: number;
  questionType: QuestionType;
  difficulty: Difficulty;
  /** LaTeX, snapshotted at evaluation time. */
  questionContent: string;
  options: ReviewOption[];
  selectedOptionIds: number[];
  correctOptionIds: number[];
  resultStatus: QuestionResultStatus;
  marksAwarded: number;
  maxMarks: number;
  /** LaTeX worked solution. Present only here, never during a live attempt. */
  solutionContent?: string;
}

/**
 * GET /attempts/{id}/review returns a BARE ARRAY of questions — not an envelope.
 * Verified against the live endpoint.
 *
 * It renders the paper AS IT WAS SAT: stem, options and key are snapshotted at
 * evaluation time, so a later admin edit or archival of a question does not alter it.
 * 409s while the attempt is still live.
 */
export type AttemptReviewResponse = ReviewQuestion[];

/** Verified against GET /me/performance. Note the field is takenAt, not submittedAt. */
export interface RecentScore {
  attemptId: number;
  testTitle: string;
  takenAt: string;
  score: number;
  maxScore: number;
  scorePercentage: number;
}

/** A row of GET /attempts/history, which is a PageResponse. */
export interface AttemptHistoryItem {
  attemptId: number;
  testId: number;
  testTitle: string;
  /**
   * Absent for a full-syllabus paper AND for a cross-chapter class test. Optional because
   * the server omits it, which the previous required typing quietly contradicted.
   */
  chapterName?: string;
  /**
   * Always present. Name the paper from this FIRST and fall back to chapterName only for a
   * PRACTICE test: "Full syllabus" is the right label for a practice paper drawn across
   * every chapter, and a lie about a three-question class test that simply spans a few.
   */
  testKind: TestKind;
  examPattern: ExamPattern;
  status: AttemptStatus;
  startedAt: string;
  totalQuestions: number;
  submittedAt?: string;
  score?: number;
  maxScore?: number;
  rankPosition?: number;
  totalCandidates?: number;
  percentile?: number;
}

export interface ChapterPerformance {
  chapterId: number;
  chapterName: string;
  attemptCount: number;
  averageScore: number;
  averageAccuracy: number;
}

export interface StudentPerformanceResponse {
  testsTaken: number;
  testsInProgress: number;
  averageScorePercentage?: number;
  bestScorePercentage?: number;
  averageAccuracy?: number;
  bestRank?: number;
  /** Oldest-first, ready to plot without re-sorting. */
  recentScores: RecentScore[];
  chapterBreakdown: ChapterPerformance[];
}
