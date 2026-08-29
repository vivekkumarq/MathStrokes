import { AbstractControl, FormGroup } from '@angular/forms';

import { ApiFailure } from '../../core/models';

/** Set by applyServerErrors so a control can render the server's own message. */
export const SERVER_ERROR_KEY = 'server';

export function serverErrorOf(control: AbstractControl | null): string | null {
  const error: unknown = control?.getError(SERVER_ERROR_KEY);
  return typeof error === 'string' ? error : null;
}

/**
 * Pushes `fieldErrors` onto the matching controls.
 *
 * This works only because the backend returns `fieldErrors[].field` as the JSON property
 * name of the request body, which is also what the form controls are named. Anything with
 * no matching control is returned so the caller can show it at form level rather than
 * silently dropping a validation message the student needs to see.
 */
export function applyServerErrors(form: FormGroup, failure: ApiFailure): string[] {
  const unmatched: string[] = [];

  for (const item of failure.fieldErrors) {
    const control = form.get(item.field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), [SERVER_ERROR_KEY]: item.message });
      control.markAsTouched();
    } else {
      unmatched.push(item.message);
    }
  }

  return unmatched;
}

/**
 * Clears previous server errors while leaving client validators intact, so a resubmit
 * doesn't show stale messages from the last attempt.
 */
export function clearServerErrors(form: FormGroup): void {
  for (const control of Object.values(form.controls)) {
    if (!control.errors || !(SERVER_ERROR_KEY in control.errors)) {
      continue;
    }
    const { [SERVER_ERROR_KEY]: _removed, ...rest } = control.errors;
    control.setErrors(Object.keys(rest).length > 0 ? rest : null);
  }
}

/** True when the control should show an error: it has one and the user has engaged with it. */
export function shouldShowError(control: AbstractControl | null): boolean {
  return !!control && control.invalid && (control.touched || control.dirty);
}

/** First human-readable message for a control, server message taking precedence. */
export function firstErrorMessage(
  control: AbstractControl | null,
  labels: Record<string, string> = {},
): string | null {
  if (!control?.errors) {
    return null;
  }
  const server = serverErrorOf(control);
  if (server) {
    return server;
  }
  const [key] = Object.keys(control.errors);
  return labels[key] ?? DEFAULT_MESSAGES[key] ?? 'This value is not valid.';
}

const DEFAULT_MESSAGES: Record<string, string> = {
  required: 'This field is required.',
  minlength: 'This value is too short.',
  maxlength: 'This value is too long.',
  pattern: 'This value is not in the expected format.',
};
