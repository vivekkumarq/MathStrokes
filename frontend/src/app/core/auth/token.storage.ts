import { Injectable } from '@angular/core';

import { AuthTokens, AuthUser } from '../models';

const TOKENS_KEY = 'iota.auth.tokens';
const USER_KEY = 'iota.auth.user';
const EXPIRES_AT_KEY = 'iota.auth.expiresAt';

/**
 * Persists the session across a refresh — which matters here because a student may
 * reload mid-exam and must land back in the same attempt.
 *
 * Every access is guarded: localStorage throws outright in some privacy modes, and a
 * storage failure must degrade to an in-memory session, not break the app.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorage {
  private memoryTokens: AuthTokens | null = null;
  private memoryUser: AuthUser | null = null;
  private memoryExpiresAt: number | null = null;

  readTokens(): AuthTokens | null {
    return this.memoryTokens ?? this.readJson<AuthTokens>(TOKENS_KEY);
  }

  readUser(): AuthUser | null {
    return this.memoryUser ?? this.readJson<AuthUser>(USER_KEY);
  }

  /** Epoch millis at which the access token expires, or null when unknown. */
  readExpiresAt(): number | null {
    if (this.memoryExpiresAt !== null) {
      return this.memoryExpiresAt;
    }
    const raw = this.readRaw(EXPIRES_AT_KEY);
    if (raw === null) {
      return null;
    }
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }

  write(tokens: AuthTokens, user: AuthUser): void {
    const expiresAt = Date.now() + tokens.expiresInSeconds * 1000;
    this.memoryTokens = tokens;
    this.memoryUser = user;
    this.memoryExpiresAt = expiresAt;
    this.writeJson(TOKENS_KEY, tokens);
    this.writeJson(USER_KEY, user);
    this.writeRaw(EXPIRES_AT_KEY, String(expiresAt));
  }

  clear(): void {
    this.memoryTokens = null;
    this.memoryUser = null;
    this.memoryExpiresAt = null;
    for (const key of [TOKENS_KEY, USER_KEY, EXPIRES_AT_KEY]) {
      try {
        localStorage.removeItem(key);
      } catch {
        // Nothing to do: the in-memory copy is already gone, which is what matters.
      }
    }
  }

  private readJson<T>(key: string): T | null {
    const raw = this.readRaw(key);
    if (raw === null) {
      return null;
    }
    try {
      return JSON.parse(raw) as T;
    } catch {
      // Corrupt entry from an older shape — drop it rather than crash on boot.
      return null;
    }
  }

  private readRaw(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private writeJson(key: string, value: unknown): void {
    this.writeRaw(key, JSON.stringify(value));
  }

  private writeRaw(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      // Quota or privacy mode. The in-memory copy keeps this tab working.
    }
  }
}
