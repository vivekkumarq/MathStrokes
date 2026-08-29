import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { SKIP_AUTH } from '../auth/auth.service';
import { AuthStore } from '../auth/auth.store';
import { toApiFailure } from './api-failure';
import { TokenRefreshCoordinator } from './token-refresh.coordinator';

function withBearer(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

/**
 * Attaches the access token and recovers from an expired one exactly once per request.
 *
 * Deliberately reactive rather than pre-emptive: it does not inspect expiry before
 * sending. The server is the authority on whether a token is still good, and a
 * pre-emptive refresh based on a skewed client clock would refresh needlessly or, worse,
 * skip a refresh that was needed.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.context.get(SKIP_AUTH)) {
    return next(request);
  }

  const store = inject(AuthStore);
  const coordinator = inject(TokenRefreshCoordinator);
  const router = inject(Router);

  const token = store.accessToken();
  const authorised = token ? withBearer(request, token) : request;

  return next(authorised).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      // An anonymous request that 401s is simply unauthorised; there is nothing to refresh.
      if (!store.refreshToken()) {
        return throwError(() => error);
      }

      const failure = toApiFailure(error);
      // 401 for a reason other than a stale token (e.g. bad credentials) is terminal.
      if (failure.code !== 'TOKEN_EXPIRED' && failure.code !== 'TOKEN_INVALID') {
        return throwError(() => error);
      }

      return coordinator.refresh().pipe(
        switchMap((fresh) => next(withBearer(request, fresh))),
        catchError((refreshError: unknown) => {
          // The refresh token is spent or revoked: the session is genuinely over.
          store.clearSession();
          void router.navigate(['/login'], {
            queryParams: { returnUrl: router.url, reason: 'session-expired' },
          });
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
