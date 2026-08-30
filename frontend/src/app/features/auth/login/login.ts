import { AuthLayout } from '../../../shared/brand/auth-layout';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { AuthStore } from '../../../core/auth/auth.store';
import { toApiFailure } from '../../../core/http/api-failure';
import { ApiFailure } from '../../../core/models';
import {
  applyServerErrors,
  clearServerErrors,
  firstErrorMessage,
  shouldShowError,
} from '../../../shared/forms/server-errors';

/** Digits only, 10-15, matching the backend's phone validation. */
const PHONE_PATTERN = /^\d{10,15}$/;

@Component({
  selector: 'app-login',
  imports: [AuthLayout, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly store = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  /** Set by the interceptor when a refresh failed, so the bounce is explained. */
  protected readonly sessionExpired = signal(
    this.route.snapshot.queryParamMap.get('reason') === 'session-expired',
  );

  protected readonly form = this.fb.nonNullable.group({
    phoneNumber: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
    password: ['', [Validators.required]],
  });

  protected readonly phone = computed(() => this.form.controls.phoneNumber);

  protected showError(name: 'phoneNumber' | 'password'): boolean {
    return shouldShowError(this.form.get(name));
  }

  protected errorFor(name: 'phoneNumber' | 'password'): string | null {
    return firstErrorMessage(this.form.get(name), {
      required: name === 'password' ? 'Enter your password.' : 'Enter your phone number.',
      pattern: 'Enter a valid phone number (10-15 digits).',
    });
  }

  protected submit(): void {
    this.formError.set(null);
    this.sessionExpired.set(false);
    clearServerErrors(this.form);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.submitting.set(true);

    this.auth
      .login({
        // The account key is stored digits-only; normalise here so a pasted number
        // with spaces or dashes doesn't read as a different account.
        phoneNumber: raw.phoneNumber.replace(/\D/g, ''),
        password: raw.password,
      })
      .subscribe({
        next: () => this.redirect(),
        error: (error: unknown) => {
          this.submitting.set(false);
          this.handleFailure(toApiFailure(error));
        },
      });
  }

  private handleFailure(failure: ApiFailure): void {
    const unmatched = applyServerErrors(this.form, failure);

    if (failure.code === 'AUTHENTICATION_FAILED') {
      // Deliberately does not say which of the two was wrong: that would let an attacker
      // enumerate which phone numbers have accounts.
      this.formError.set('Incorrect phone number or password.');
      return;
    }
    if (failure.code === 'RATE_LIMITED') {
      this.formError.set('Too many attempts. Please wait a moment and try again.');
      return;
    }
    if (failure.fieldErrors.length > 0) {
      this.formError.set(unmatched.length > 0 ? unmatched[0] : null);
      return;
    }
    this.formError.set(failure.message);
  }

  private redirect(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    if (returnUrl) {
      void this.router.navigateByUrl(returnUrl);
      return;
    }
    void this.router.navigate([this.store.isAdmin() ? '/admin' : '/student']);
  }
}
