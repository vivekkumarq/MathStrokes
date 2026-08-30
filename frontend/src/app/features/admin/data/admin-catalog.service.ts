import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import { ChapterResponse, MarkingSchemeResponse, SubjectResponse } from '../../../core/models';

/** Subjects, chapters and marking schemes for the authoring forms. */
@Injectable({ providedIn: 'root' })
export class AdminCatalogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_CONFIG).baseUrl;

  subjects(): Observable<SubjectResponse[]> {
    return this.http.get<SubjectResponse[]>(`${this.baseUrl}/admin/catalog/subjects`);
  }

  chapters(): Observable<ChapterResponse[]> {
    return this.http.get<ChapterResponse[]>(`${this.baseUrl}/admin/catalog/chapters`);
  }

  markingSchemes(): Observable<MarkingSchemeResponse[]> {
    return this.http.get<MarkingSchemeResponse[]>(`${this.baseUrl}/admin/marking-schemes`);
  }
}
