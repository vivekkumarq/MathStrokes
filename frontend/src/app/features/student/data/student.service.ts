import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import {
  AttemptHistoryItem,
  PageResponse,
  StudentPerformanceResponse,
} from '../../../core/models';

/** Student dashboard data: aggregate performance and past attempts. */
@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_CONFIG).baseUrl;

  performance(): Observable<StudentPerformanceResponse> {
    return this.http.get<StudentPerformanceResponse>(`${this.baseUrl}/me/performance`);
  }

  history(page = 0, size = 5): Observable<PageResponse<AttemptHistoryItem>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AttemptHistoryItem>>(`${this.baseUrl}/attempts/history`, {
      params,
    });
  }
}
