import { HttpClient, HttpContext, HttpContextToken } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import {
  AuthResponse,
  ForgotPasswordInitiateRequest,
  ForgotPasswordInitiateResponse,
  ForgotPasswordVerifyRequest,
  ForgotPasswordVerifyResponse,
  LoginRequest,
  MessageResponse,
  ResetPasswordRequest,
  SecurityQuestion,
  StudentRegisterRequest,
} from '../models';
import { AuthStore } from './auth.store';

/**
 * Marks a request the auth interceptor must leave alone: no Bearer header, and a 401
 * must not trigger a refresh attempt. Login and refresh themselves are the obvious
 * cases — retrying a failed login as if the token were stale would loop.
 */
export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);

export function skipAuth(): HttpContext {
  return new HttpContext().set(SKIP_AUTH, true);
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly store = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly baseUrl = inject(API_CONFIG).baseUrl;

  /**
   * Public canonical list for the register form.
   *
   * The server stores and later displays the question TEXT, not the id, so registration
   * sends `securityQuestion` as the text. Membership is not hard-validated server-side —
   * deliberately, so this list can grow without invalidating older accounts.
   */
  securityQuestions(): Observable<SecurityQuestion[]> {
    return this.http.get<SecurityQuestion[]>(`${this.baseUrl}/auth/security-questions`, {
      context: skipAuth(),
    });
  }

  registerStudent(request: StudentRegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/student/register`, request, {
        context: skipAuth(),
      })
      .pipe(tap((response) => this.adopt(response)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/login`, request, { context: skipAuth() })
      .pipe(tap((response) => this.adopt(response)));
  }

  /**
   * Rotating refresh: the response carries a NEW refresh token and the old one is dead,
   * so the store must be updated even though the caller usually only wants the access token.
   */
  refresh(refreshToken: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(
        `${this.baseUrl}/auth/refresh`,
        { refreshToken },
        { context: skipAuth() },
      )
      .pipe(tap((response) => this.adopt(response)));
  }

  /**
   * Signs the user out.
   *
   * Tears the local session down and navigates FIRST, then fires the revocation without
   * waiting for it. Every caller already discarded the result — they cleared the session
   * on both success and failure — so blocking only made the user wait to learn an outcome
   * nobody acts on. On a sleeping backend that wait is 30-50 seconds spent on a page they
   * asked to leave.
   *
   * Nothing is lost by not waiting: the refresh token is gone from storage the moment the
   * session clears, the access token expires on its own, and if the request does land the
   * server revokes exactly as before.
   */
  signOut(): void {
    // Read the token before clearing, or there is nothing left to revoke.
    const refreshToken = this.store.refreshToken();

    this.store.clearSession();
    void this.router.navigate(['/login']);

    if (refreshToken) {
      this.revoke(refreshToken).subscribe({ next: () => undefined, error: () => undefined });
    }
  }

  /** Best-effort server-side revocation. Callers should prefer signOut(). */
  revoke(refreshToken: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/auth/logout`, { refreshToken });
  }

  forgotPasswordInitiate(
    request: ForgotPasswordInitiateRequest,
  ): Observable<ForgotPasswordInitiateResponse> {
    return this.http.post<ForgotPasswordInitiateResponse>(
      `${this.baseUrl}/auth/forgot-password/initiate`,
      request,
      { context: skipAuth() },
    );
  }

  forgotPasswordVerify(
    request: ForgotPasswordVerifyRequest,
  ): Observable<ForgotPasswordVerifyResponse> {
    return this.http.post<ForgotPasswordVerifyResponse>(
      `${this.baseUrl}/auth/forgot-password/verify`,
      request,
      { context: skipAuth() },
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/auth/reset-password`, request, {
      context: skipAuth(),
    });
  }

  private adopt(response: AuthResponse): void {
    const { user, ...tokens } = response;
    this.store.setSession(tokens, user);
  }
}
