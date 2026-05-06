import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ConfirmReceiptRequest,
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

  markPickedUp(id: number): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(`${this.base}/${id}/picked-up`, {});
  }

  markInTransit(id: number): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(`${this.base}/${id}/in-transit`, {});
  }

  confirmDelivery(id: number, body: ConfirmReceiptRequest): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(`${this.base}/${id}/delivered`, body);
  }

  downloadContract(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/contract`, { responseType: 'blob' });
  }
}
