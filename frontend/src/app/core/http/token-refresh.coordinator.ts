import { Injectable, inject } from '@angular/core';
import { Observable, finalize, map, shareReplay, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { AuthStore } from '../auth/auth.store';

/**
 * Serialises token refresh.
 *
 * An exam screen fires several requests at once (autosave, palette, timer heartbeat). If
 * the access token expires they all get a 401 together, and each would otherwise start
 * its own refresh. With a ROTATING refresh token that is not merely wasteful — the first
 * refresh invalidates the token the others are holding, so the rest fail and the student
 * is logged out mid-exam. One in-flight refresh, shared by every waiter.
 */
@Injectable({ providedIn: 'root' })
export class TokenRefreshCoordinator {
  private readonly auth = inject(AuthService);
  private readonly store = inject(AuthStore);

  private inFlight: Observable<string> | null = null;

  /** Emits the new access token. Concurrent callers share a single network round-trip. */
  refresh(): Observable<string> {
    if (this.inFlight) {
      return this.inFlight;
    }

    const refreshToken = this.store.refreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    this.inFlight = this.auth.refresh(refreshToken).pipe(
      map((response) => response.accessToken),
      finalize(() => {
        this.inFlight = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    return this.inFlight;
  }
}
