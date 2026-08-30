import { ExamPattern } from './enums';

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
}

/** Query for GET /tests. Both filters are optional and combine. */
export interface TestQuery {
  chapterId?: number;
  examPattern?: ExamPattern;
}
