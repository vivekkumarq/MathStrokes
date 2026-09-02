import {
  Difficulty,
  ExamPattern,
  QuestionStatus,
  QuestionType,
  TestGenerationMode,
  TestKind,
  TestStatus,
} from './enums';

/**
 * Admin authoring contract (/admin/**). Verified against the live API.
 *
 * Optional fields are `?:` because the API omits nulls rather than sending them.
 */

export interface QuestionOptionRequest {
  optionKey: string;
  content: string;
  displayOrder: number;
  isCorrect: boolean;
}

export interface QuestionOptionResponse extends QuestionOptionRequest {
  id: number;
}

export interface QuestionRequest {
  chapterId: number;
  examPattern: ExamPattern;
  difficulty: Difficulty;
  questionType: QuestionType;
  /** LaTeX source. Dollar delimiters only; stored verbatim, never rendered server-side. */
  questionContent: string;
  solutionContent?: string;
  /** Absent means the chapter/pattern default scheme applies. */
  markingSchemeId?: number;
  options: QuestionOptionRequest[];
}

export interface QuestionResponse {
  id: number;
  subjectId: number;
  subjectName: string;
  chapterId: number;
  chapterName: string;
  examPattern: ExamPattern;
  difficulty: Difficulty;
  questionType: QuestionType;
  questionContent: string;
  solutionContent?: string;
  status: QuestionStatus;
  markingSchemeId?: number;
  markingSchemeName?: string;
  createdByName: string;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
  /** Optimistic-locking version. Send it back on update to detect a concurrent edit. */
  version: number;
  options: QuestionOptionResponse[];
}

/** The list row. Carries a preview rather than the full stem, so the grid stays light. */
export interface QuestionSummaryResponse {
  id: number;
  chapterName: string;
  examPattern: ExamPattern;
  difficulty: Difficulty;
  questionType: QuestionType;
  questionPreview: string;
  status: QuestionStatus;
  optionCount: number;
  updatedAt: string;
  version: number;
}

/** Filters for GET /admin/questions. Every field is optional and they combine. */
export interface QuestionQuery {
  subjectId?: number;
  chapterId?: number;
  examPattern?: ExamPattern;
  difficulty?: Difficulty;
  questionType?: QuestionType;
  status?: QuestionStatus;
  search?: string;
  page?: number;
  size?: number;
}

export interface MarkingSchemeResponse {
  id: number;
  name: string;
  description?: string;
  examPattern: ExamPattern;
  questionType: QuestionType;
  configuration: string;
  active: boolean;
}

export interface AdminTestRequest {
  title: string;
  description?: string;
  /** Omit for a full-syllabus paper, which draws across every chapter. */
  chapterId?: number;
  examPattern: ExamPattern;
  durationMinutes: number;
  questionCount: number;
  generationMode: TestGenerationMode;
  /**
   * The difficulty blueprint for a randomly drawn paper. Every band is optional on the
   * server (@PositiveOrZero, not @NotNull) and a null band means "no constraint", so a
   * hand-picked class test omits all three rather than sending a split it never uses.
   */
  easyCount?: number;
  mediumCount?: number;
  hardCount?: number;
  maxAttemptsPerStudent: number;
  /** Defaults to PRACTICE server-side, so existing papers keep their meaning. */
  testKind?: TestKind;
  /**
   * ISO-8601 instants bounding when a class test may be sat. Both optional: a teacher may
   * flag a paper live with no window at all. Publishing stays the master switch — the
   * window is a second, independent gate, so no scheduler has to fire for a test to open.
   */
  scheduledStartAt?: string;
  scheduledEndAt?: string;
}

/**
 * The exact paper for a hand-picked test. Array order IS the question order, so reordering
 * the tray and saving is the whole reorder operation - there is no separate order field
 * that could disagree with the sequence.
 */
export interface TestQuestionsRequest {
  questionIds: number[];
}

export interface AdminTestResponse {
  id: number;
  title: string;
  description?: string;
  subjectId: number;
  subjectName: string;
  /** Absent on a full-syllabus paper. */
  chapterId?: number;
  chapterName?: string;
  examPattern: ExamPattern;
  status: TestStatus;
  durationMinutes: number;
  questionCount: number;
  /** How many questions are actually materialised. Only meaningful for FIXED_SET. */
  attachedQuestionCount: number;
  generationMode: TestGenerationMode;
  maxAttemptsPerStudent: number;
  rankingEnabled: boolean;
  testKind: TestKind;
  /** Absent when the teacher set no window. */
  scheduledStartAt?: string;
  scheduledEndAt?: string;
  publishedAt?: string;
  createdAt: string;
  version: number;
}

/** GET /admin/analytics/dashboard. Flat counters; all present. */
export interface AdminDashboardResponse {
  totalStudents: number;
  activeStudentsLast30Days: number;
  totalQuestions: number;
  publishedQuestions: number;
  draftQuestions: number;
  archivedQuestions: number;
  totalTests: number;
  publishedTests: number;
  totalAttempts: number;
  attemptsInProgress: number;
  attemptsLast7Days: number;
}

/** A row of GET /admin/students. Paged, with a `search` filter. */
export interface StudentSummaryResponse {
  id: number;
  fullName: string;
  phoneNumber: string;
  enabled: boolean;
  /** Absent for a student who has registered but never signed in. */
  lastLoginAt?: string;
  registeredAt: string;
  attemptCount: number;
}

export interface StudentQuery {
  search?: string;
  page?: number;
  size?: number;
}
