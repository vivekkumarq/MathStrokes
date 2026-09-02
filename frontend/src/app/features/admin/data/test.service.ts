import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import {
  AdminTestRequest,
  AdminTestResponse,
  PageResponse,
  QuestionSummaryResponse,
} from '../../../core/models';

/** Admin test management: blueprint a test from the question bank, then publish it. */
@Injectable({ providedIn: 'root' })
export class TestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_CONFIG).baseUrl;

  list(page = 0, size = 50): Observable<PageResponse<AdminTestResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminTestResponse>>(`${this.baseUrl}/admin/tests`, {
      params,
    });
  }

  get(id: number): Observable<AdminTestResponse> {
    return this.http.get<AdminTestResponse>(`${this.baseUrl}/admin/tests/${id}`);
  }

  create(request: AdminTestRequest): Observable<AdminTestResponse> {
    return this.http.post<AdminTestResponse>(`${this.baseUrl}/admin/tests`, request);
  }

  update(id: number, request: AdminTestRequest): Observable<AdminTestResponse> {
    return this.http.put<AdminTestResponse>(`${this.baseUrl}/admin/tests/${id}`, request);
  }

  /** The questions currently pinned to a hand-picked paper, in paper order. */
  questions(id: number): Observable<QuestionSummaryResponse[]> {
    return this.http.get<QuestionSummaryResponse[]>(
      `${this.baseUrl}/admin/tests/${id}/questions`,
    );
  }

  /**
   * Replaces the paper wholesale. Array order becomes question order.
   *
   * Publishing a fixed-set test only draws questions when none are attached yet, so a paper
   * saved here survives publish untouched - that guard is what makes hand-picking work
   * without a second code path through publish.
   */
  setQuestions(id: number, questionIds: number[]): Observable<AdminTestResponse> {
    return this.http.put<AdminTestResponse>(`${this.baseUrl}/admin/tests/${id}/questions`, {
      questionIds,
    });
  }

  publish(id: number): Observable<AdminTestResponse> {
    return this.http.post<AdminTestResponse>(`${this.baseUrl}/admin/tests/${id}/publish`, {});
  }

  close(id: number): Observable<AdminTestResponse> {
    return this.http.post<AdminTestResponse>(`${this.baseUrl}/admin/tests/${id}/close`, {});
  }

  archive(id: number): Observable<AdminTestResponse> {
    return this.http.post<AdminTestResponse>(`${this.baseUrl}/admin/tests/${id}/archive`, {});
  }
}
