/** Mirrors com.mathstrokes.common.dto and common.exception.ErrorCode. */

/** Mirrors FieldErrorItem. `field` is expected to match the request-body property name. */
export interface FieldErrorItem {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

/**
 * The single error envelope returned by every failing endpoint.
 * Serialised NON_EMPTY on the server, so absent collections arrive as `undefined`.
 */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: FieldErrorItem[];
}

export interface MessageResponse {
  message: string;
}

/** Transport-friendly page wrapper. The server never serialises Spring's Page directly. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export const ERROR_CODES = [
  'VALIDATION_ERROR',
  'BUSINESS_RULE_VIOLATION',
  'RESOURCE_NOT_FOUND',
  'DUPLICATE_RESOURCE',
  'AUTHENTICATION_FAILED',
  'TOKEN_INVALID',
  'TOKEN_EXPIRED',
  'ACCESS_DENIED',
  'ATTEMPT_EXPIRED',
  'ATTEMPT_ALREADY_FINALISED',
  'NOT_ENOUGH_QUESTIONS',
  'RATE_LIMITED',
  'INTERNAL_ERROR',
] as const;
export type ErrorCode = (typeof ERROR_CODES)[number];

const ERROR_CODE_SET = new Set<string>(ERROR_CODES);

export function isErrorCode(value: string | undefined): value is ErrorCode {
  return value !== undefined && ERROR_CODE_SET.has(value);
}

/**
 * Normalised failure the whole app works with, so no component ever touches
 * HttpErrorResponse directly.
 */
export interface ApiFailure {
  /** Parsed from the envelope's `error` when it is a known code, else 'INTERNAL_ERROR'. */
  code: ErrorCode;
  /** HTTP status. 0 means the request never reached the server (offline/CORS/DNS). */
  status: number;
  /** Safe to show to a user. */
  message: string;
  fieldErrors: FieldErrorItem[];
  path?: string;
  timestamp?: string;
  /** True when the browser could not reach the API at all. */
  offline: boolean;
}

/** Field errors keyed by field name, ready to push onto a reactive form. */
export function fieldErrorMap(failure: ApiFailure): Record<string, string> {
  const map: Record<string, string> = {};
  for (const item of failure.fieldErrors) {
    // First error per field wins; a field with several violations shows the first.
    map[item.field] ??= item.message;
  }
  return map;
}
