import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TransportCompanyResponse } from '../models/transport-company.models';

@Injectable({ providedIn: 'root' })
export class TransportCompanyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/transport-companies`;

  getAll(): Observable<TransportCompanyResponse[]> {
    return this.http.get<TransportCompanyResponse[]>(this.base);
  }

  getById(id: number): Observable<TransportCompanyResponse> {
    return this.http.get<TransportCompanyResponse>(`${this.base}/${id}`);
  }
}
