import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  API_CONFIG,
  AttemptResponse,
  AttemptResultResponse,
  AttemptReviewResponse,
  SaveAnswerRequest,
  SaveAnswerResponse,
  TestQuery,
  TestSummaryResponse,
} from '../../core';

/**
 * Everything the student examination flow talks to.
 *
 * Uses the same HttpClient and API_CONFIG as AuthService, so the auth interceptor attaches
 * the bearer token and the single-flight refresh coordinator covers these calls too. There
 * is no second HTTP stack here on purpose: during an exam the palette, the autosave and the
 * clock all fire together, and they must share one refresh.
 */
@Injectable({ providedIn: 'root' })
export class ExamService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_CONFIG).baseUrl;

  /** Published tests, with whether this student may start each one. Carries no questions. */
  availableTests(query: TestQuery = {}): Observable<TestSummaryResponse[]> {
    let params = new HttpParams();
    if (query.chapterId != null) {
      params = params.set('chapterId', query.chapterId);
    }
    if (query.examPattern) {
      params = params.set('examPattern', query.examPattern);
    }
    return this.http.get<TestSummaryResponse[]>(`${this.baseUrl}/tests`, { params });
  }

  /**
   * Starts a test, or resumes the attempt already in flight on it.
   *
   * The server decides which: calling this again during a live attempt returns the same
   * paper, in the same order, with the same deadline. That is what makes a refresh safe.
   */
  startOrResume(testId: number): Observable<AttemptResponse> {
    return this.http.post<AttemptResponse>(`${this.baseUrl}/attempts`, { testId });
  }

  /** The attempt in flight, if any. The server answers 204 when there is none. */
  activeAttempt(): Observable<AttemptResponse | null> {
    return this.http.get<AttemptResponse | null>(`${this.baseUrl}/attempts/active`);
  }

  attempt(attemptId: number): Observable<AttemptResponse> {
    return this.http.get<AttemptResponse>(`${this.baseUrl}/attempts/${attemptId}`);
  }

  /**
   * Autosave. The request carries the COMPLETE selection, and the ack carries the state the
   * server actually holds — which is not always what was sent, if a newer write won.
   */
  saveAnswer(attemptId: number, request: SaveAnswerRequest): Observable<SaveAnswerResponse> {
    return this.http.put<SaveAnswerResponse>(
      `${this.baseUrl}/attempts/${attemptId}/answers`,
      request,
    );
  }

  /** Idempotent: an attempt the expiry sweep already finalised returns its result, not an error. */
  submit(attemptId: number): Observable<AttemptResultResponse> {
    return this.http.post<AttemptResultResponse>(
      `${this.baseUrl}/attempts/${attemptId}/submit`,
      {},
    );
  }

  result(attemptId: number): Observable<AttemptResultResponse> {
    return this.http.get<AttemptResultResponse>(`${this.baseUrl}/attempts/${attemptId}/result`);
  }

  /** A bare array, not an envelope. The only payload carrying the answer key. */
  review(attemptId: number): Observable<AttemptReviewResponse> {
    return this.http.get<AttemptReviewResponse>(`${this.baseUrl}/attempts/${attemptId}/review`);
  }
}
