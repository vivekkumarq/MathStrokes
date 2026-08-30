import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import {
  MessageResponse,
  PageResponse,
  QuestionQuery,
  QuestionRequest,
  QuestionResponse,
  QuestionSummaryResponse,
} from '../../../core/models';

/**
 * Admin question bank. Follows the same shape as AuthService: base URL from API_CONFIG,
 * no skipAuth (so the interceptor attaches the Bearer token and can refresh on 401),
 * and errors mapped by the caller with toApiFailure.
 */
@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_CONFIG).baseUrl;

  list(query: QuestionQuery = {}): Observable<PageResponse<QuestionSummaryResponse>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      // An empty filter must be omitted entirely, not sent as an empty string —
      // `status=` would be a bad enum value rather than "no filter".
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<QuestionSummaryResponse>>(`${this.baseUrl}/admin/questions`, {
      params,
    });
  }

  get(id: number): Observable<QuestionResponse> {
    return this.http.get<QuestionResponse>(`${this.baseUrl}/admin/questions/${id}`);
  }

  create(request: QuestionRequest): Observable<QuestionResponse> {
    return this.http.post<QuestionResponse>(`${this.baseUrl}/admin/questions`, request);
  }

  update(id: number, request: QuestionRequest): Observable<QuestionResponse> {
    return this.http.put<QuestionResponse>(`${this.baseUrl}/admin/questions/${id}`, request);
  }

  publish(id: number): Observable<QuestionResponse | MessageResponse> {
    return this.http.post<QuestionResponse>(`${this.baseUrl}/admin/questions/${id}/publish`, {});
  }

  toDraft(id: number): Observable<QuestionResponse | MessageResponse> {
    return this.http.post<QuestionResponse>(`${this.baseUrl}/admin/questions/${id}/draft`, {});
  }

  archive(id: number): Observable<QuestionResponse | MessageResponse> {
    return this.http.post<QuestionResponse>(`${this.baseUrl}/admin/questions/${id}/archive`, {});
  }
}
