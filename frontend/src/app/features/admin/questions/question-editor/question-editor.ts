import { Component, computed, inject, input, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';

import { toApiFailure } from '../../../../core/http/api-failure';
import {
  ApiFailure,
  ChapterResponse,
  DIFFICULTIES,
  EXAM_PATTERNS,
  QUESTION_TYPES,
  QuestionRequest,
  QuestionResponse,
  QuestionStatus,
} from '../../../../core/models';
import { applyServerErrors, clearServerErrors } from '../../../../shared/forms/server-errors';
import { MathContent } from '../../../../shared/math/math-content';
import { AdminCatalogService } from '../../data/admin-catalog.service';
import { QuestionService } from '../../data/question.service';
import { AdminShell } from '../../layout/admin-shell';

const OPTION_KEYS = ['A', 'B', 'C', 'D'] as const;

/**
 * The correct-answer rule depends on questionType, so it lives on the form group rather
 * than on the options array: SINGLE_CORRECT needs exactly one, MULTIPLE_CORRECT needs at
 * least one. The server enforces this too; this is just a faster, kinder failure.
 */
function correctAnswerRule(group: AbstractControl): ValidationErrors | null {
  const type = group.get('questionType')?.value;
  const options = group.get('options') as FormArray | null;
  if (!options) {
    return null;
  }
  const correct = options.controls.filter((control) => control.get('isCorrect')?.value === true);

  if (correct.length === 0) {
    return { noCorrectOption: true };
  }
  if (type === 'SINGLE_CORRECT' && correct.length > 1) {
    return { tooManyCorrectOptions: true };
  }
  return null;
}

@Component({
  selector: 'app-question-editor',
  imports: [AdminShell, MathContent, ReactiveFormsModule],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.scss',
})
export class QuestionEditor {
  private readonly fb = inject(FormBuilder);
  private readonly questions = inject(QuestionService);
  private readonly catalog = inject(AdminCatalogService);
  private readonly router = inject(Router);

  /** Route param, bound via withComponentInputBinding. Absent means "new question". */
  readonly id = input<string | undefined>(undefined);

  protected readonly patterns = EXAM_PATTERNS;
  protected readonly difficulties = DIFFICULTIES;
  protected readonly types = QUESTION_TYPES;
  protected readonly optionKeys = OPTION_KEYS;

  protected readonly chapters = signal<ChapterResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly status = signal<QuestionStatus | null>(null);
  protected readonly loaded = signal<QuestionResponse | null>(null);

  protected readonly isNew = computed(() => this.id() === undefined || this.id() === 'new');
  protected readonly heading = computed(() => (this.isNew() ? 'New question' : `Question #${this.id()}`));

  protected readonly form = this.fb.nonNullable.group(
    {
      chapterId: [0, [Validators.required, Validators.min(1)]],
      examPattern: ['JEE_MAIN' as (typeof EXAM_PATTERNS)[number], [Validators.required]],
      difficulty: ['MEDIUM' as (typeof DIFFICULTIES)[number], [Validators.required]],
      questionType: ['SINGLE_CORRECT' as (typeof QUESTION_TYPES)[number], [Validators.required]],
      questionContent: ['', [Validators.required]],
      solutionContent: [''],
      options: this.fb.array(OPTION_KEYS.map((key, index) => this.optionGroup(key, index))),
    },
    { validators: correctAnswerRule },
  );

  protected get options(): FormArray<FormGroup> {
    return this.form.controls.options as FormArray<FormGroup>;
  }

  /** Live preview source — the whole point of authoring LaTeX in a textarea. */
  protected readonly stemPreview = signal('');
  protected readonly solutionPreview = signal('');

  constructor() {
    this.catalog.chapters().subscribe({
      next: (chapters) => {
        this.chapters.set(chapters);
        if (this.isNew() && chapters.length > 0 && this.form.controls.chapterId.value === 0) {
          this.form.controls.chapterId.setValue(chapters[0].id);
        }
      },
      error: () => undefined,
    });

    // Keep previews in step with the textareas without wiring a subscription per field.
    this.form.controls.questionContent.valueChanges.subscribe((value) =>
      this.stemPreview.set(value ?? ''),
    );
    this.form.controls.solutionContent.valueChanges.subscribe((value) =>
      this.solutionPreview.set(value ?? ''),
    );

    const id = this.id();
    if (id !== undefined && id !== 'new') {
      this.load(Number(id));
    }
  }

  protected optionPreview(index: number): string {
    return (this.options.at(index).get('content')?.value as string) ?? '';
  }

  /**
   * Selecting a correct option in SINGLE_CORRECT mode clears the others, so the form
   * cannot sit in a state the server would reject.
   */
  protected onCorrectChange(index: number): void {
    if (this.form.controls.questionType.value !== 'SINGLE_CORRECT') {
      return;
    }
    this.options.controls.forEach((control, i) => {
      if (i !== index) {
        control.get('isCorrect')?.setValue(false, { emitEvent: false });
      }
    });
  }

  /** Switching to single-correct collapses any extra selections to the first one. */
  protected onTypeChange(): void {
    if (this.form.controls.questionType.value !== 'SINGLE_CORRECT') {
      return;
    }
    let seen = false;
    this.options.controls.forEach((control) => {
      const isCorrect = control.get('isCorrect');
      if (isCorrect?.value === true) {
        if (seen) {
          isCorrect.setValue(false, { emitEvent: false });
        }
        seen = true;
      }
    });
  }

  protected save(): void {
    this.formError.set(null);
    this.notice.set(null);
    clearServerErrors(this.form);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.hasError('noCorrectOption')) {
        this.formError.set('Mark at least one option as correct.');
      } else if (this.form.hasError('tooManyCorrectOptions')) {
        this.formError.set('A single-correct question can have only one correct option.');
      }
      return;
    }

    const request = this.toRequest();
    this.saving.set(true);

    const id = this.id();
    const call =
      id !== undefined && id !== 'new'
        ? this.questions.update(Number(id), request)
        : this.questions.create(request);

    call.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.status.set(saved.status);
        this.loaded.set(saved);
        if (this.isNew()) {
          void this.router.navigate(['/admin/questions', saved.id]);
        } else {
          this.notice.set('Saved.');
        }
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.handleFailure(toApiFailure(err));
      },
    });
  }

  protected transition(action: 'publish' | 'draft' | 'archive'): void {
    const id = this.id();
    if (id === undefined || id === 'new') {
      return;
    }
    this.formError.set(null);
    this.notice.set(null);
    this.saving.set(true);

    const call =
      action === 'publish'
        ? this.questions.publish(Number(id))
        : action === 'draft'
          ? this.questions.toDraft(Number(id))
          : this.questions.archive(Number(id));

    call.subscribe({
      next: (result) => {
        this.saving.set(false);
        if ('status' in result) {
          this.status.set(result.status);
        }
        this.notice.set(`Question ${action === 'draft' ? 'moved to draft' : action + 'ed'}.`);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.handleFailure(toApiFailure(err));
      },
    });
  }

  protected back(): void {
    void this.router.navigate(['/admin/questions']);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.questions.get(id).subscribe({
      next: (question) => {
        this.loading.set(false);
        this.loaded.set(question);
        this.status.set(question.status);
        this.form.patchValue({
          chapterId: question.chapterId,
          examPattern: question.examPattern,
          difficulty: question.difficulty,
          questionType: question.questionType,
          questionContent: question.questionContent,
          solutionContent: question.solutionContent ?? '',
        });
        // Options come back ordered; patch by index so the A-D keys stay stable.
        const sorted = [...question.options].sort((a, b) => a.displayOrder - b.displayOrder);
        sorted.forEach((option, index) => {
          if (index < this.options.length) {
            this.options.at(index).patchValue({
              optionKey: option.optionKey,
              content: option.content,
              displayOrder: option.displayOrder,
              isCorrect: option.isCorrect,
            });
          }
        });
        this.stemPreview.set(question.questionContent);
        this.solutionPreview.set(question.solutionContent ?? '');
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.formError.set(toApiFailure(err).message);
      },
    });
  }

  private toRequest(): QuestionRequest {
    const raw = this.form.getRawValue();
    const solution = raw.solutionContent.trim();
    return {
      chapterId: raw.chapterId,
      examPattern: raw.examPattern,
      difficulty: raw.difficulty,
      questionType: raw.questionType,
      questionContent: raw.questionContent,
      // Omit rather than send an empty string: the field is optional server-side.
      ...(solution === '' ? {} : { solutionContent: solution }),
      // Bracket access: a FormArray<FormGroup> raw value is index-signature typed, and
      // displayOrder is taken from position so the A-D order is always consistent.
      options: raw.options.map((option, index) => ({
        optionKey: option['optionKey'] as string,
        content: option['content'] as string,
        displayOrder: index,
        isCorrect: option['isCorrect'] as boolean,
      })),
    };
  }

  private handleFailure(failure: ApiFailure): void {
    const unmatched = applyServerErrors(this.form, failure);
    if (unmatched.length > 0) {
      this.formError.set(unmatched[0]);
      return;
    }
    if (failure.fieldErrors.length === 0) {
      this.formError.set(failure.message);
    }
  }

  private optionGroup(key: string, index: number): FormGroup {
    return this.fb.nonNullable.group({
      optionKey: [key],
      content: ['', [Validators.required]],
      displayOrder: [index],
      isCorrect: [false],
    });
  }
}
