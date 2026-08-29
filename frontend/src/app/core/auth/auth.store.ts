import { Injectable, computed, inject, signal } from '@angular/core';

import { AuthTokens, AuthUser, RoleName } from '../models';
import { TokenStorage } from './token.storage';

/**
 * Single source of truth for who is signed in.
 *
 * State is seeded synchronously from storage in the constructor so that route guards
 * evaluated on the very first navigation already see a restored session — otherwise a
 * refresh on a protected page bounces the user to /login before rehydration lands.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly storage = inject(TokenStorage);

  private readonly userSignal = signal<AuthUser | null>(null);
  private readonly tokensSignal = signal<AuthTokens | null>(null);

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.userSignal() !== null && this.tokensSignal() !== null);
  readonly roles = computed<RoleName[]>(() => this.userSignal()?.roles ?? []);
  readonly isAdmin = computed(() => this.roles().includes('ROLE_ADMIN'));
  readonly isStudent = computed(() => this.roles().includes('ROLE_STUDENT'));
  readonly displayName = computed(() => this.userSignal()?.fullName ?? '');

  constructor() {
    const tokens = this.storage.readTokens();
    const user = this.storage.readUser();
    if (tokens && user) {
      this.tokensSignal.set(tokens);
      this.userSignal.set(user);
    }
  }

  /** Read outside the reactive graph — the interceptor needs a plain value, not a signal read. */
  accessToken(): string | null {
    return this.tokensSignal()?.accessToken ?? null;
  }

  refreshToken(): string | null {
    return this.tokensSignal()?.refreshToken ?? null;
  }

  /** True when the access token is expired or close enough that it would die in flight. */
  isAccessTokenNearExpiry(skewSeconds: number): boolean {
    const expiresAt = this.storage.readExpiresAt();
    if (expiresAt === null) {
      return false;
    }
    return Date.now() >= expiresAt - skewSeconds * 1000;
  }

  setSession(tokens: AuthTokens, user: AuthUser): void {
    this.storage.write(tokens, user);
    this.tokensSignal.set(tokens);
    this.userSignal.set(user);
  }

  clearSession(): void {
    this.storage.clear();
    this.tokensSignal.set(null);
    this.userSignal.set(null);
  }

  hasAnyRole(required: readonly RoleName[]): boolean {
    if (required.length === 0) {
      return true;
    }
    const held = this.roles();
    return required.some((role) => held.includes(role));
  }
}
