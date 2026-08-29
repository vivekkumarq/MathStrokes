import { HttpClient, HttpContext, HttpContextToken } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
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

  logout(): Observable<MessageResponse> {
    const refreshToken = this.store.refreshToken();
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
