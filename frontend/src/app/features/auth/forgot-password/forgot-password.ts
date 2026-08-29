import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { toApiFailure } from '../../../core/http/api-failure';
import { ApiFailure } from '../../../core/models';
import { firstErrorMessage, shouldShowError } from '../../../shared/forms/server-errors';

const PHONE_PATTERN = /^\d{10,15}$/;

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  if (!password || !confirm) {
    return null;
  }
  return password === confirm ? null : { passwordMismatch: true };
}

type Step = 'identify' | 'answer' | 'reset' | 'done';

/**
 * Three server steps, one screen.
 *
 * Recovery is by security question, not a mailed link — there is no email in the system.
 * Step 1 returns ONLY the question, so a phone number alone reveals nothing beyond it.
 */
@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss',
})
export class ForgotPassword {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly step = signal<Step>('identify');
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly securityQuestion = signal('');

  private resetToken = '';

  protected readonly identifyForm = this.fb.nonNullable.group({
    phoneNumber: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
  });

  protected readonly answerForm = this.fb.nonNullable.group({
    securityAnswer: ['', [Validators.required]],
  });

  protected readonly resetForm = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  protected showError(form: 'identify' | 'answer' | 'reset', name: string): boolean {
    return shouldShowError(this.formOf(form).get(name));
  }

  protected errorFor(form: 'identify' | 'answer' | 'reset', name: string): string | null {
    return firstErrorMessage(this.formOf(form).get(name), {
      required: 'This field is required.',
      minlength: 'Use at least 8 characters.',
      pattern: 'Enter a valid phone number (10-15 digits).',
    });
  }

  protected get mismatch(): boolean {
    const confirm = this.resetForm.controls.confirmPassword;
    return this.resetForm.hasError('passwordMismatch') && (confirm.touched || confirm.dirty);
  }

  protected submitIdentify(): void {
    if (!this.begin(this.identifyForm)) {
      return;
    }
    const phoneNumber = this.identifyForm.getRawValue().phoneNumber.replace(/\D/g, '');

    this.auth.forgotPasswordInitiate({ phoneNumber }).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.securityQuestion.set(response.securityQuestion);
        this.step.set('answer');
      },
      error: (error: unknown) => this.fail(error),
    });
  }

  protected submitAnswer(): void {
    if (!this.begin(this.answerForm)) {
      return;
    }
    const phoneNumber = this.identifyForm.getRawValue().phoneNumber.replace(/\D/g, '');
    const { securityAnswer } = this.answerForm.getRawValue();

    this.auth.forgotPasswordVerify({ phoneNumber, securityAnswer }).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.resetToken = response.resetToken;
        this.step.set('reset');
      },
      error: (error: unknown) => this.fail(error),
    });
  }

  protected submitReset(): void {
    if (!this.begin(this.resetForm)) {
      return;
    }
    const { newPassword, confirmPassword } = this.resetForm.getRawValue();

    this.auth
      .resetPassword({ resetToken: this.resetToken, newPassword, confirmPassword })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.step.set('done');
        },
        error: (error: unknown) => this.fail(error),
      });
  }

  protected goToLogin(): void {
    void this.router.navigate(['/login']);
  }

  /**
   * Returned as the untyped FormGroup on purpose: the three forms have different value
   * shapes, and a union of typed groups has no callable .get() signature.
   */
  private formOf(name: 'identify' | 'answer' | 'reset'): FormGroup {
    if (name === 'identify') {
      return this.identifyForm;
    }
    return name === 'answer' ? this.answerForm : this.resetForm;
  }

  /** Shared submit preamble: returns false when the form should not be sent. */
  private begin(form: AbstractControl): boolean {
    this.formError.set(null);
    if (form.invalid) {
      form.markAllAsTouched();
      return false;
    }
    this.submitting.set(true);
    return true;
  }

  private fail(error: unknown): void {
    this.submitting.set(false);
    const failure: ApiFailure = toApiFailure(error);

    if (failure.code === 'RESOURCE_NOT_FOUND') {
      // Same wording whether or not the account exists, so this cannot be used to test
      // which phone numbers are registered.
      this.formError.set('If that account exists, we could not verify those details.');
      return;
    }
    if (failure.code === 'TOKEN_EXPIRED' || failure.code === 'TOKEN_INVALID') {
      this.formError.set('That reset link has expired. Please start again.');
      this.step.set('identify');
      return;
    }
    this.formError.set(failure.message);
  }
}
