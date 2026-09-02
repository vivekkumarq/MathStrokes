import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { toApiFailure } from '../../../core/http/api-failure';
import {
  AdminTestRequest,
  AdminTestResponse,
  ApiFailure,
  ChapterResponse,
  EXAM_PATTERNS,
  TEST_GENERATION_MODES,
  TEST_KINDS,
} from '../../../core/models';
import { applyServerErrors, clearServerErrors } from '../../../shared/forms/server-errors';
import { AdminCatalogService } from '../data/admin-catalog.service';
import { TestService } from '../data/test.service';
import { AdminShell } from '../layout/admin-shell';

/**
 * The difficulty split must add up to the paper length, otherwise the blueprint cannot be
 * satisfied. Checked here so the author sees it immediately rather than as a 409.
 */
function blueprintAddsUp(group: AbstractControl): ValidationErrors | null {
  // A class test is hand-picked, so nothing is drawn and there is no split to satisfy.
  if (group.get('testKind')?.value === 'CLASS_TEST') {
    return null;
  }
  const total = Number(group.get('questionCount')?.value ?? 0);
  const parts =
    Number(group.get('easyCount')?.value ?? 0) +
    Number(group.get('mediumCount')?.value ?? 0) +
    Number(group.get('hardCount')?.value ?? 0);
  return parts === total ? null : { blueprintMismatch: { total, parts } };
}

/**
 * A class test is scheduled in India, for a class sitting in India, so its wall-clock time is
 * read as IST no matter where the teacher's laptop thinks it is. Fixing the offset here is
 * what keeps the instant we send and the "Opens on 4 September at 4:00 PM" the server renders
 * back describing the same moment - a browser-local reading would drift apart from it the
 * moment anyone travelled.
 */
const IST_OFFSET = '+05:30';

/** datetime-local ("2026-09-04T16:00") to an offset-qualified instant the server can parse. */
function toInstant(local: string): string | undefined {
  if (local.trim() === '') {
    return undefined;
  }
  const withSeconds = local.length === 16 ? `${local}:00` : local;
  return `${withSeconds}${IST_OFFSET}`;
}

@Component({
  selector: 'app-admin-test-list',
  imports: [AdminShell, ReactiveFormsModule, RouterLink],
  templateUrl: './test-list.html',
  styleUrl: './test-list.scss',
})
export class AdminTestList {
  private readonly fb = inject(FormBuilder);
  private readonly tests = inject(TestService);
  private readonly catalog = inject(AdminCatalogService);

  protected readonly patterns = EXAM_PATTERNS;
  protected readonly modes = TEST_GENERATION_MODES;

  protected readonly rows = signal<AdminTestResponse[]>([]);
  protected readonly chapters = signal<ChapterResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly showForm = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group(
    {
      title: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      // 0 is the 'all chapters' sentinel; it is omitted from the request entirely.
      chapterId: [0],
      examPattern: ['JEE_MAIN' as (typeof EXAM_PATTERNS)[number], [Validators.required]],
      durationMinutes: [60, [Validators.required, Validators.min(1)]],
      questionCount: [25, [Validators.required, Validators.min(1)]],
      // FIXED_SET materialises one shared paper, which is what makes ranking fair.
      generationMode: ['FIXED_SET' as (typeof TEST_GENERATION_MODES)[number], [Validators.required]],
      easyCount: [8, [Validators.required, Validators.min(0)]],
      mediumCount: [9, [Validators.required, Validators.min(0)]],
      hardCount: [8, [Validators.required, Validators.min(0)]],
      maxAttemptsPerStudent: [5, [Validators.required, Validators.min(1)]],
      testKind: ['PRACTICE' as (typeof TEST_KINDS)[number], [Validators.required]],
      // Wall-clock IST, empty for "no window". Publishing is the live switch either way.
      scheduledStartAt: [''],
      scheduledEndAt: [''],
    },
    { validators: blueprintAddsUp },
  );

  protected readonly blueprintTotal = computed(() => {
    const v = this.form.getRawValue();
    return Number(v.easyCount) + Number(v.mediumCount) + Number(v.hardCount);
  });

  /** Drives which half of the form is relevant: a drawn blueprint, or a hand-picked paper. */
  protected readonly kindValue = signal<(typeof TEST_KINDS)[number]>('PRACTICE');
  protected readonly isClassTest = computed(() => this.kindValue() === 'CLASS_TEST');

  /**
   * A class test whose paper has not been picked yet.
   *
   * Publishing one would be actively wrong rather than merely premature: publish draws a
   * random paper when no questions are attached, so a teacher who went live too early would
   * hand their class a random paper while believing they had chosen it. The button is
   * withheld until the paper exists.
   */
  protected needsPaper(test: AdminTestResponse): boolean {
    return test.testKind === 'CLASS_TEST' && test.attachedQuestionCount === 0;
  }

  /** Renders a scheduled window in IST, the zone it was entered in. */
  protected windowLabel(test: AdminTestResponse): string | null {
    if (test.scheduledStartAt === undefined && test.scheduledEndAt === undefined) {
      return null;
    }
    const fmt = (iso: string) =>
      new Date(iso).toLocaleString('en-IN', {
        timeZone: 'Asia/Kolkata',
        day: 'numeric',
        month: 'short',
        hour: 'numeric',
        minute: '2-digit',
      });
    if (test.scheduledStartAt !== undefined && test.scheduledEndAt !== undefined) {
      return `${fmt(test.scheduledStartAt)} - ${fmt(test.scheduledEndAt)}`;
    }
    return test.scheduledStartAt !== undefined
      ? `From ${fmt(test.scheduledStartAt)}`
      : `Until ${fmt(test.scheduledEndAt!)}`;
  }

  constructor() {
    this.form.controls.testKind.valueChanges.subscribe((kind) => {
      this.kindValue.set(kind);
      if (kind === 'CLASS_TEST') {
        // A hand-picked paper only exists under FIXED_SET; drawing per attempt would
        // discard the teacher's choices at the moment each student starts.
        this.form.controls.generationMode.setValue('FIXED_SET');
      }
      // The split is validated at group level, so re-run it against the new kind.
      this.form.updateValueAndValidity();
    });

    this.catalog.chapters().subscribe({
      next: (chapters) => {
        this.chapters.set(chapters);

      },
      error: () => undefined,
    });
    this.load();
  }

  protected toggleForm(): void {
    this.showForm.update((open) => !open);
    this.error.set(null);
    this.notice.set(null);
  }

  protected create(): void {
    this.error.set(null);
    this.notice.set(null);
    clearServerErrors(this.form);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.hasError('blueprintMismatch')) {
        this.error.set(
          `The difficulty split adds up to ${this.blueprintTotal()}, but the paper is ${this.form.controls.questionCount.value} questions.`,
        );
      }
      return;
    }

    const raw = this.form.getRawValue();
    const classTest = raw.testKind === 'CLASS_TEST';

    const start = toInstant(raw.scheduledStartAt);
    const end = toInstant(raw.scheduledEndAt);
    if (start !== undefined && end !== undefined && end <= start) {
      this.error.set('The test cannot close before it opens. Check the scheduled window.');
      return;
    }

    const request: AdminTestRequest = {
      ...raw,
      description: raw.description.trim() === '' ? undefined : raw.description.trim(),
      // Omitting chapterId is what makes it a full-syllabus paper.
      chapterId: raw.chapterId === 0 ? undefined : raw.chapterId,
      testKind: raw.testKind,
      scheduledStartAt: start,
      scheduledEndAt: end,
      // A hand-picked paper draws nothing, so it carries no difficulty blueprint. The
      // server treats an absent band as "no constraint", and the count is replaced by the
      // length of the paper once the questions are saved.
      easyCount: classTest ? undefined : raw.easyCount,
      mediumCount: classTest ? undefined : raw.mediumCount,
      hardCount: classTest ? undefined : raw.hardCount,
    };

    this.busy.set(true);
    this.tests.create(request).subscribe({
      next: (created) => {
        this.busy.set(false);
        this.notice.set(
          created.testKind === 'CLASS_TEST'
            ? `Created "${created.title}". Choose its questions next — it cannot go live until the paper is picked.`
            : `Created "${created.title}". Publish it to make it visible to students.`,
        );
        this.showForm.set(false);
        this.load();
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.handleFailure(toApiFailure(err));
      },
    });
  }

  protected transition(test: AdminTestResponse, action: 'publish' | 'close' | 'archive'): void {
    this.error.set(null);
    this.notice.set(null);

    if (action === 'publish' && this.needsPaper(test)) {
      this.error.set(
        `"${test.title}" has no questions yet. Choose its paper first, or publishing would draw a random one.`,
      );
      return;
    }

    this.busy.set(true);

    const call =
      action === 'publish'
        ? this.tests.publish(test.id)
        : action === 'close'
          ? this.tests.close(test.id)
          : this.tests.archive(test.id);

    call.subscribe({
      next: () => {
        this.busy.set(false);
        const what =
          action === 'publish'
            ? 'is now live for students'
            : action === 'close'
              ? 'has been taken offline'
              : 'was archived';
        this.notice.set(`"${test.title}" ${what}.`);
        this.load();
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.error.set(toApiFailure(err).message);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.tests.list().subscribe({
      next: (page) => {
        this.rows.set(page.content);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(toApiFailure(err).message);
      },
    });
  }

  private handleFailure(failure: ApiFailure): void {
    const unmatched = applyServerErrors(this.form, failure);
    this.error.set(unmatched.length > 0 ? unmatched[0] : failure.message);
    // Keep the form open so the author can correct it rather than losing their input.
    this.showForm.set(true);
  }
}
