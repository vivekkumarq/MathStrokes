import { TestKind } from './enums';

/** The parts of any paper-describing payload this label needs. */
interface PaperLike {
  testKind: TestKind;
  /** Absent for a full-syllabus paper and for a cross-chapter class test. */
  chapterName?: string;
}

/**
 * How to name the scope of a paper in one line.
 *
 * Reads testKind FIRST and only falls back to the chapter for a practice paper. Both a
 * full-syllabus practice test and a hand-picked class test spanning several chapters arrive
 * with no chapterName, so `chapterName ?? 'Full syllabus'` cannot tell them apart — and it
 * resolved the wrong way, describing a three-question class test as a full syllabus paper on
 * the student's own dashboard.
 *
 * One function rather than the same conditional in four templates: the four disagreeing is
 * exactly how the original mislabel survived in some screens after being fixed in others.
 */
export function paperScopeLabel(paper: PaperLike): string {
  if (paper.testKind === 'CLASS_TEST') {
    return 'Class test';
  }
  return paper.chapterName ?? 'Full syllabus';
}
