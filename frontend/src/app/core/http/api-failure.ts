import { HttpErrorResponse } from '@angular/common/http';

import { ApiErrorResponse, ApiFailure, ErrorCode, FieldErrorItem, isErrorCode } from '../models';

const GENERIC_MESSAGE = 'Something went wrong. Please try again.';
const OFFLINE_MESSAGE = 'Cannot reach the server. Check your connection and try again.';

function isApiErrorResponse(body: unknown): body is ApiErrorResponse {
  return (
    typeof body === 'object' &&
    body !== null &&
    'status' in body &&
    'message' in body &&
    'error' in body
  );
}

/**
 * Collapses anything HttpClient can throw into the one shape the app reacts to.
 *
 * Branching is on the `error` enum name, never on message text — the backend treats
 * that name as the stable contract and the message as human-facing prose.
 */
export function toApiFailure(error: unknown): ApiFailure {
  if (!(error instanceof HttpErrorResponse)) {
    return {
      code: 'INTERNAL_ERROR',
      status: 0,
      message: GENERIC_MESSAGE,
      fieldErrors: [],
      offline: false,
    };
  }

  // status 0 means the request never reached the server at all: offline, DNS, CORS,
  // or the backend simply isn't running yet.
  if (error.status === 0) {
    return {
      code: 'INTERNAL_ERROR',
      status: 0,
      message: OFFLINE_MESSAGE,
      fieldErrors: [],
      offline: true,
    };
  }

  const body: unknown = error.error;

  if (isApiErrorResponse(body)) {
    const code: ErrorCode = isErrorCode(body.error) ? body.error : 'INTERNAL_ERROR';
    const fieldErrors: FieldErrorItem[] = body.fieldErrors ?? [];
    return {
      code,
      status: body.status || error.status,
      message: body.message || GENERIC_MESSAGE,
      fieldErrors,
      path: body.path,
      timestamp: body.timestamp,
      offline: false,
    };
  }

  // A failure that never made it through the GlobalExceptionHandler — a container-level
  // 502/504, or a proxy's HTML error page. Don't surface that raw to a student.
  return {
    code: 'INTERNAL_ERROR',
    status: error.status,
    message: GENERIC_MESSAGE,
    fieldErrors: [],
    offline: false,
  };
}

/** The access token is stale or bad; the session may be recoverable via refresh. */
export function isTokenFailure(failure: ApiFailure): boolean {
  return failure.code === 'TOKEN_EXPIRED' || failure.code === 'TOKEN_INVALID';
}

/** The attempt can no longer accept this write. The runner must resync from the server. */
export function isAttemptClosedFailure(failure: ApiFailure): boolean {
  return failure.code === 'ATTEMPT_EXPIRED' || failure.code === 'ATTEMPT_ALREADY_FINALISED';
}
