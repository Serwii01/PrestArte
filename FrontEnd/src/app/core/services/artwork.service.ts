import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, Observable, of } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ArtworkResponse,
  CreateArtworkRequest,
} from '../models/artwork.models';

@Injectable({ providedIn: 'root' })
export class ArtworkService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/artworks`;
  private readonly filesBase = `${environment.apiBaseUrl}/files`;

  getAll(): Observable<ArtworkResponse[]> {
    return this.http.get<ArtworkResponse[]>(this.base);
  }

  getById(id: number): Observable<ArtworkResponse> {
    return this.http.get<ArtworkResponse>(`${this.base}/${id}`);
  }

  /** Obras del coleccionista actual. Endpoint real del backend. */
  getByCollector(collectorId: number): Observable<ArtworkResponse[]> {
    return this.http.get<ArtworkResponse[]>(`${this.base}/collector/${collectorId}`);
  }

  create(body: CreateArtworkRequest): Observable<ArtworkResponse> {
    return this.http.post<ArtworkResponse>(this.base, body);
  }

  /** Sube un archivo asociado a una obra. multipart/form-data. */
  uploadFile(artworkId: number, file: File): Observable<string> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post(`${this.filesBase}/upload/artwork/${artworkId}`, data, {
      responseType: 'text',
    });
  }

  /** Sube varios archivos en paralelo. */
  uploadFiles(artworkId: number, files: File[]): Observable<string[]> {
    if (files.length === 0) return of([]);
    return forkJoin(files.map((f) => this.uploadFile(artworkId, f)));
  }

  /** URL pública (GET autenticado) para mostrar el archivo. */
  fileUrl(fileId: string): string {
    return `${this.filesBase}/${fileId}`;
  }
}
