import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ConditionReportRequest,
  ProposeQuoteRequest,
  ShipmentResponse,
} from '../models/shipment.models';
import { CancelLoanRequest } from '../models/loan.models';

@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/shipments`;

  getById(id: number): Observable<ShipmentResponse> {
    return this.http.get<ShipmentResponse>(`${this.base}/${id}`);
  }

  getByCompany(companyId: number): Observable<ShipmentResponse[]> {
    return this.http.get<ShipmentResponse[]>(`${this.base}/company/${companyId}`);
  }

  proposeQuote(id: number, body: ProposeQuoteRequest): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(`${this.base}/${id}/quote`, body);
  }

  approveQuote(id: number): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(`${this.base}/${id}/approve-quote`, {});
  }

  rejectQuote(id: number, body: CancelLoanRequest = {}): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(`${this.base}/${id}/reject-quote`, body);
  }

  /** Confirma la recogida con el informe de estado de salida y sus fotos. */
  markPickedUp(id: number, report: ConditionReportRequest, photos: File[]): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(
      `${this.base}/${id}/picked-up`,
      this.reportForm(report, photos),
    );
  }

  markInTransit(id: number): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(`${this.base}/${id}/in-transit`, {});
  }

  /** Confirma la entrega con el informe de estado de llegada y sus fotos. */
  confirmDelivery(id: number, report: ConditionReportRequest, photos: File[]): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(
      `${this.base}/${id}/delivered`,
      this.reportForm(report, photos),
    );
  }

  /** Multipart: parte `data` en JSON + una parte `photos` por foto. */
  private reportForm(report: ConditionReportRequest, photos: File[]): FormData {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(report)], { type: 'application/json' }));
    photos.forEach((p) => form.append('photos', p));
    return form;
  }

  downloadContract(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/contract`, { responseType: 'blob' });
  }
}
