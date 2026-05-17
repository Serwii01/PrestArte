import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  TransportCompanyProfile,
  TransportCompanyResponse,
  UpdateTransportCompanyRequest,
} from '../models/transport-company.models';

@Injectable({ providedIn: 'root' })
export class TransportCompanyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/transport-companies`;

  /** Listado público (perfil resumido). */
  getAll(): Observable<TransportCompanyProfile[]> {
    return this.http.get<TransportCompanyProfile[]>(this.base);
  }

  /** Ficha pública de una empresa. */
  getById(id: number): Observable<TransportCompanyProfile> {
    return this.http.get<TransportCompanyProfile>(`${this.base}/${id}`);
  }

  /**
   * Compatibilidad: para los desplegables de selección al aceptar un préstamo,
   * el front consumía `TransportCompanyResponse`. Ahora el endpoint devuelve
   * `TransportCompanyProfile` con `companyName` + `coverageArea`, que son los
   * campos que usaba el desplegable.
   */
  getAllForSelect(): Observable<TransportCompanyResponse[]> {
    return this.http.get<TransportCompanyResponse[]>(this.base);
  }

  updateProfile(
    id: number,
    body: UpdateTransportCompanyRequest,
  ): Observable<TransportCompanyProfile> {
    return this.http.put<TransportCompanyProfile>(`${this.base}/${id}/profile`, body);
  }
}
