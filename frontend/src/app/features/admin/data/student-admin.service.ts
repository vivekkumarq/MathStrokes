import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import {
  AttemptHistoryItem,
  PageResponse,
  StudentPerformanceResponse,
  StudentQuery,
  StudentSummaryResponse,
} from '../../../core/models';

/** Admin view of the student roster. */
@Injectable({ providedIn: 'root' })
export class StudentAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_CONFIG).baseUrl;

  list(query: StudentQuery = {}): Observable<PageResponse<StudentSummaryResponse>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      // An empty search must be omitted, not sent as an empty string.
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<StudentSummaryResponse>>(`${this.baseUrl}/admin/students`, {
      params,
    });
  }

  /**
   * A named student's attempts. The admin counterpart of /attempts/history, which reads
   * the caller's own identity — these take the id explicitly and sit behind ROLE_ADMIN.
   * Same DTO, so no extra model is needed.
   */
  attempts(id: number, page = 0, size = 20): Observable<PageResponse<AttemptHistoryItem>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AttemptHistoryItem>>(
      `${this.baseUrl}/admin/students/${id}/attempts`,
      { params },
    );
  }

  performance(id: number): Observable<StudentPerformanceResponse> {
    return this.http.get<StudentPerformanceResponse>(
      `${this.baseUrl}/admin/students/${id}/performance`,
    );
  }

  /** Enable or disable a student's access. */
  setEnabled(id: number, enabled: boolean): Observable<StudentSummaryResponse> {
    return this.http.patch<StudentSummaryResponse>(
      `${this.baseUrl}/admin/students/${id}/enabled`,
      { enabled },
    );
  }
}
