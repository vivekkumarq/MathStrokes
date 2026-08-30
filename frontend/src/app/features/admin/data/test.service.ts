import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import { AdminTestRequest, AdminTestResponse, PageResponse } from '../../../core/models';

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

  create(request: AdminTestRequest): Observable<AdminTestResponse> {
    return this.http.post<AdminTestResponse>(`${this.baseUrl}/admin/tests`, request);
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
