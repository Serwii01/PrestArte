import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  DueDiligencePendingItem,
  DueDiligenceRequest,
  ProvenanceEntryRequest,
  ProvenanceResponse,
} from '../models/provenance.models';

@Injectable({ providedIn: 'root' })
export class ProvenanceService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  get(artworkId: number): Observable<ProvenanceResponse> {
    return this.http.get<ProvenanceResponse>(`${this.base}/artworks/${artworkId}/provenance`);
  }

  declareTitle(artworkId: number): Observable<ProvenanceResponse> {
    return this.http.post<ProvenanceResponse>(`${this.base}/artworks/${artworkId}/title-declaration`, {});
  }

  addEntry(artworkId: number, data: ProvenanceEntryRequest, file: File | null): Observable<ProvenanceResponse> {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (file) form.append('file', file);
    return this.http.post<ProvenanceResponse>(`${this.base}/artworks/${artworkId}/provenance`, form);
  }

  deleteEntry(artworkId: number, entryId: number): Observable<ProvenanceResponse> {
    return this.http.delete<ProvenanceResponse>(`${this.base}/artworks/${artworkId}/provenance/${entryId}`);
  }

  /* ===== Administración ===== */

  pending(): Observable<DueDiligencePendingItem[]> {
    return this.http.get<DueDiligencePendingItem[]>(`${this.base}/admin/due-diligence/pending`);
  }

  recordDueDiligence(artworkId: number, body: DueDiligenceRequest): Observable<ProvenanceResponse> {
    return this.http.post<ProvenanceResponse>(`${this.base}/admin/artworks/${artworkId}/due-diligence`, body);
  }
}
