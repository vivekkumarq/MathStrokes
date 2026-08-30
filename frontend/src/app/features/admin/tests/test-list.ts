import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';

import { toApiFailure } from '../../../core/http/api-failure';
import {
  AdminTestRequest,
  AdminTestResponse,
  ApiFailure,
  ChapterResponse,
  EXAM_PATTERNS,
  TEST_GENERATION_MODES,
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
  const total = Number(group.get('questionCount')?.value ?? 0);
  const parts =
    Number(group.get('easyCount')?.value ?? 0) +
    Number(group.get('mediumCount')?.value ?? 0) +
    Number(group.get('hardCount')?.value ?? 0);
  return parts === total ? null : { blueprintMismatch: { total, parts } };
}

@Component({
  selector: 'app-admin-test-list',
  imports: [AdminShell, ReactiveFormsModule],
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
    },
    { validators: blueprintAddsUp },
  );

  protected readonly blueprintTotal = computed(() => {
    const v = this.form.getRawValue();
    return Number(v.easyCount) + Number(v.mediumCount) + Number(v.hardCount);
  });

  constructor() {
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
    const request: AdminTestRequest = {
      ...raw,
      description: raw.description.trim() === '' ? undefined : raw.description.trim(),
      // Omitting chapterId is what makes it a full-syllabus paper.
      chapterId: raw.chapterId === 0 ? undefined : raw.chapterId,
    };

    this.busy.set(true);
    this.tests.create(request).subscribe({
      next: (created) => {
        this.busy.set(false);
        this.notice.set(`Created "${created.title}". Publish it to make it visible to students.`);
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
        this.notice.set(`"${test.title}" ${action === 'archive' ? 'archived' : action + 'ed'}.`);
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
