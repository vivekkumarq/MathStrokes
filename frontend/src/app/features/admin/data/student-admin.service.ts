import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import { PageResponse, StudentQuery, StudentSummaryResponse } from '../../../core/models';

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

  /** Enable or disable a student's access. */
  setEnabled(id: number, enabled: boolean): Observable<StudentSummaryResponse> {
    return this.http.patch<StudentSummaryResponse>(
      `${this.baseUrl}/admin/students/${id}/enabled`,
      { enabled },
    );
  }
}
