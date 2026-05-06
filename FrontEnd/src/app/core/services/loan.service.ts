import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AcceptLoanRequest,
  CancelLoanRequest,
  CreateLoanRequest,
  LoanResponse,
} from '../models/loan.models';

@Injectable({ providedIn: 'root' })
export class LoanService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/loan-requests`;

  /* ===== Lectura ===== */

  getAll(): Observable<LoanResponse[]> {
    return this.http.get<LoanResponse[]>(this.base);
  }

  getById(id: number): Observable<LoanResponse> {
    return this.http.get<LoanResponse>(`${this.base}/${id}`);
  }

  getByCollector(collectorId: number): Observable<LoanResponse[]> {
    return this.http.get<LoanResponse[]>(`${this.base}/collector/${collectorId}`);
  }

  getByFoundation(foundationId: number): Observable<LoanResponse[]> {
    return this.http.get<LoanResponse[]>(`${this.base}/foundation/${foundationId}`);
  }

  /* ===== Acciones del flujo ===== */

  create(body: CreateLoanRequest): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(this.base, body);
  }

  accept(id: number, body: AcceptLoanRequest): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/accept`, body);
  }

  reject(id: number): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/reject`, {});
  }

  cancel(id: number, body: CancelLoanRequest = {}): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/cancel`, body);
  }

  markReadyForPickup(id: number): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/ready-for-pickup`, {});
  }

  startReturn(id: number): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/start-return`, {});
  }

  /* ===== Contrato PDF ===== */

  downloadContract(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/contract`, { responseType: 'blob' });
  }
}
