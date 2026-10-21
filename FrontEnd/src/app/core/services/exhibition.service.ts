import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ExhibitionCompatibility, ExhibitionRequest, ExhibitionResponse } from '../models/exhibition.models';

@Injectable({ providedIn: 'root' })
export class ExhibitionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/exhibitions`;

  /** Exposiciones de la fundación con sesión. */
  getMine(): Observable<ExhibitionResponse[]> {
    return this.http.get<ExhibitionResponse[]>(`${this.base}/mine`);
  }

  getById(id: number): Observable<ExhibitionResponse> {
    return this.http.get<ExhibitionResponse>(`${this.base}/${id}`);
  }

  create(body: ExhibitionRequest): Observable<ExhibitionResponse> {
    return this.http.post<ExhibitionResponse>(this.base, body);
  }

  update(id: number, body: ExhibitionRequest): Observable<ExhibitionResponse> {
    return this.http.put<ExhibitionResponse>(`${this.base}/${id}`, body);
  }

  /** Compatibilidad de todas las obras con nuestras instalaciones, también entre sí. */
  getCompatibility(id: number): Observable<ExhibitionCompatibility> {
    return this.http.get<ExhibitionCompatibility>(`${this.base}/${id}/compatibility`);
  }
}
