import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { toApiFailure } from '../../../core/http/api-failure';
import { ApiFailure, SecurityQuestion } from '../../../core/models';
import {
  applyServerErrors,
  clearServerErrors,
  firstErrorMessage,
  shouldShowError,
} from '../../../shared/forms/server-errors';

const PHONE_PATTERN = /^\d{10,15}$/;

/**
 * Used only when GET /auth/security-questions cannot be reached.
 *
 * The canonical list is served by the backend; this local copy exists so registration
 * still works if that call fails, rather than presenting an empty select. The server
 * accepts any non-blank string up to 255 chars, so a fallback value still registers
 * successfully — that leniency is a compatibility hedge, not an invitation to free text.
 */
const FALLBACK_SECURITY_QUESTIONS: SecurityQuestion[] = [
  { id: 'first-school', text: 'What was the name of your first school?' },
  { id: 'mothers-maiden-name', text: 'What is your mother’s maiden name?' },
  { id: 'first-pet', text: 'What was the name of your first pet?' },
  { id: 'birth-city', text: 'What city were you born in?' },
  { id: 'favourite-book', text: 'What is your favourite book?' },
];

/** Cross-field check. Reported on the group so it survives either field changing. */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  if (!password || !confirm) {
    return null;
  }
  return password === confirm ? null : { passwordMismatch: true };
}

type FieldName =
  | 'fullName'
  | 'phoneNumber'
  | 'password'
  | 'confirmPassword'
  | 'securityQuestion'
  | 'securityAnswer';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly questions = signal<SecurityQuestion[]>(FALLBACK_SECURITY_QUESTIONS);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      phoneNumber: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      // Holds the question TEXT, not the id: the server stores the text and shows it
      // back at reset time.
      securityQuestion: [FALLBACK_SECURITY_QUESTIONS[0].text, [Validators.required]],
      securityAnswer: ['', [Validators.required, Validators.maxLength(120)]],
    },
    { validators: passwordsMatch },
  );

  constructor() {
    this.auth.securityQuestions().subscribe({
      next: (questions) => {
        if (questions.length === 0) {
          return;
        }
        this.questions.set(questions);
        // Only move the selection if the student has not already chosen one.
        if (!this.form.controls.securityQuestion.dirty) {
          this.form.controls.securityQuestion.setValue(questions[0].text);
        }
      },
      // Keep the fallback list. A student must be able to register even if this
      // lookup is down, and the server does not require list membership.
      error: () => undefined,
    });
  }

  protected showError(name: FieldName): boolean {
    return shouldShowError(this.form.get(name));
  }

  protected errorFor(name: FieldName): string | null {
    return firstErrorMessage(this.form.get(name), {
      required: 'This field is required.',
      minlength: 'Use at least 8 characters.',
      pattern: 'Enter a valid phone number (10-15 digits).',
    });
  }

  protected get mismatch(): boolean {
    const confirm = this.form.controls.confirmPassword;
    return this.form.hasError('passwordMismatch') && (confirm.touched || confirm.dirty);
  }

  protected submit(): void {
    this.formError.set(null);
    clearServerErrors(this.form);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.submitting.set(true);

    this.auth
      .registerStudent({ ...raw, phoneNumber: raw.phoneNumber.replace(/\D/g, '') })
      .subscribe({
        // Register signs the student straight in, so go to their dashboard.
        next: () => void this.router.navigate(['/student']),
        error: (error: unknown) => {
          this.submitting.set(false);
          this.handleFailure(toApiFailure(error));
        },
      });
  }

  private handleFailure(failure: ApiFailure): void {
    const unmatched = applyServerErrors(this.form, failure);

    if (failure.code === 'DUPLICATE_RESOURCE') {
      this.form.controls.phoneNumber.setErrors({
        server: 'An account with this phone number already exists.',
      });
      this.form.controls.phoneNumber.markAsTouched();
      this.formError.set(null);
      return;
    }
    if (failure.fieldErrors.length > 0) {
      this.formError.set(unmatched.length > 0 ? unmatched[0] : null);
      return;
    }
    this.formError.set(failure.message);
  }
}
