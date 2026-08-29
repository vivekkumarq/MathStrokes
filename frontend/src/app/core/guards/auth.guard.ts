import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { AuthStore } from '../auth/auth.store';
import { RoleName } from '../models';

/** Requires a signed-in user. Preserves the attempted URL so login can return them to it. */
export const authGuard: CanActivateFn = (_route, state): boolean | UrlTree => {
  const store = inject(AuthStore);
  const router = inject(Router);

  if (store.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/**
 * Requires one of the given roles.
 *
 * A signed-in user who lacks the role is sent to their own home rather than to /login —
 * bouncing an authenticated student to a login screen reads as a bug, not a denial.
 * This is convenience only; the backend enforces authorisation for real.
 */
export function roleGuard(...roles: RoleName[]): CanActivateFn {
  return (_route, state): boolean | UrlTree => {
    const store = inject(AuthStore);
    const router = inject(Router);

    if (!store.isAuthenticated()) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    if (store.hasAnyRole(roles)) {
      return true;
    }
    return router.createUrlTree([store.isAdmin() ? '/admin' : '/student']);
  };
}

/** Keeps a signed-in user off the login/register screens. */
export const guestGuard: CanActivateFn = (): boolean | UrlTree => {
  const store = inject(AuthStore);
  const router = inject(Router);

  if (!store.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree([store.isAdmin() ? '/admin' : '/student']);
};

/**
 * Landing redirect for '' — always returns a UrlTree, never true, so the empty path
 * resolves to a real screen instead of rendering an empty outlet.
 */
export const landingGuard: CanActivateFn = (): UrlTree => {
  const store = inject(AuthStore);
  const router = inject(Router);

  if (!store.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }
  return router.createUrlTree([store.isAdmin() ? '/admin' : '/student']);
};
