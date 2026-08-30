import {
  Difficulty,
  ExamPattern,
  QuestionStatus,
  QuestionType,
  TestGenerationMode,
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
  easyCount: number;
  mediumCount: number;
  hardCount: number;
  maxAttemptsPerStudent: number;
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
