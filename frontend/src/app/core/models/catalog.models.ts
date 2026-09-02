import { ExamPattern, TestKind } from './enums';

/**
 * Catalog + test browsing.
 *
 * IMPORTANT: the API serialises with non-null inclusion, so a null field is OMITTED from
 * the JSON rather than sent as null. Every conditionally-present field is therefore
 * modelled `field?: T`, and absence — not null — is the signal to branch on.
 */

export interface SubjectResponse {
  id: number;
  name: string;
  code: string;
  description?: string;
  active: boolean;
  displayOrder: number;
  chapterCount: number;
}

export interface ChapterResponse {
  id: number;
  subjectId: number;
  subjectName: string;
  name: string;
  description?: string;
  active: boolean;
  displayOrder: number;
}

export interface TestSummaryResponse {
  id: number;
  title: string;
  description?: string;
  subjectName: string;
  /**
   * Absent on a FULL-SYLLABUS paper, which draws across every chapter. Present on a
   * chapter test. Absence is the signal, per the API's omit-nulls rule.
   */
  chapterId?: number;
  chapterName?: string;
  examPattern: ExamPattern;
  questionCount: number;
  durationMinutes: number;
  rankingEnabled: boolean;
  /** Primitive int on the server with a 1..100 CHECK constraint: always present. */
  maxAttemptsPerStudent: number;
  attemptsUsed: number;
  canStart: boolean;
  /**
   * Present only when canStart is false. Student-facing prose written by the backend —
   * render verbatim, never derive replacement wording client-side.
   */
  unavailableReason?: string;
  /** Present only while an attempt on this test is in flight. Absence means nothing to resume. */
  activeAttemptId?: number;
  /**
   * PRACTICE for the always-on self-study papers, CLASS_TEST for one a teacher scheduled.
   * Students see the two in separate groups.
   */
  testKind: TestKind;
  /**
   * When a scheduled class test opens and closes, as ISO-8601 instants. Both absent when the
   * teacher set no window, in which case a published paper is simply open.
   *
   * These are for DISPLAY only - whether the paper can actually be started is `canStart`,
   * decided server-side. Never gate the start button on a clock comparison done here: the
   * student's device clock is not trustworthy and would let a wrong one open a test early.
   */
  scheduledStartAt?: string;
  scheduledEndAt?: string;
}

/** Query for GET /tests. Both filters are optional and combine. */
export interface TestQuery {
  chapterId?: number;
  examPattern?: ExamPattern;
}
