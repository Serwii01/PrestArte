import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CompatibilityReport,
  FacilityReportRequest,
  FacilityReportResponse,
} from '../models/facility.models';

@Injectable({ providedIn: 'root' })
export class FacilityReportService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/facility-report`;

  /** Informe de la fundación con sesión, o null si aún no lo ha rellenado (204). */
  getMine(): Observable<FacilityReportResponse | null> {
    return this.http
      .get<FacilityReportResponse>(`${this.base}/me`, { observe: 'response' })
      .pipe(map((res) => (res.status === 204 ? null : res.body)));
  }

  saveMine(body: FacilityReportRequest): Observable<FacilityReportResponse> {
    return this.http.put<FacilityReportResponse>(`${this.base}/me`, body);
  }

  /** Adjunta el PDF oficial (UKRG, AAM...). */
  uploadDocument(file: File): Observable<FacilityReportResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<FacilityReportResponse>(`${this.base}/me/document`, form);
  }

  getForFoundation(foundationId: number): Observable<FacilityReportResponse> {
    return this.http.get<FacilityReportResponse>(`${this.base}/foundation/${foundationId}`);
  }

  /** Compatibilidad de una obra con las instalaciones de la fundación con sesión. */
  artworkCompatibility(artworkId: number, startDate?: string): Observable<CompatibilityReport> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    return this.http.get<CompatibilityReport>(
      `${environment.apiBaseUrl}/artworks/${artworkId}/compatibility`,
      { params },
    );
  }
}
