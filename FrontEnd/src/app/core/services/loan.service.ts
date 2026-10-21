import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AcceptLoanRequest,
  CancelLoanRequest,
  CreateLoanRequest,
  ExportPermitRequest,
  InsuranceCertificateRequest,
  LoanDocumentsResponse,
  LoanEventResponse,
  LoanResponse,
} from '../models/loan.models';
import { ConditionReportRequest, ConditionReportResponse } from '../models/shipment.models';
import { CompatibilityReport } from '../models/facility.models';
import { CostEstimate, SupervisionCheckpoint, SupervisionStage } from '../models/cost.models';
import { LoanReviews, RatingSummary } from '../models/review.models';

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

  /** Historial del préstamo (cambios de estado e hitos). */
  getHistory(id: number): Observable<LoanEventResponse[]> {
    return this.http.get<LoanEventResponse[]>(`${this.base}/${id}/events`);
  }

  /** Compatibilidad entre los requisitos de la obra y las instalaciones de la fundación. */
  getCompatibility(id: number): Observable<CompatibilityReport> {
    return this.http.get<CompatibilityReport>(`${this.base}/${id}/compatibility`);
  }

  /* ===== Ampliación, certificado y valoraciones ===== */

  requestExtension(id: number, newEndDate: string, reason?: string): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/extension`, { newEndDate, reason });
  }

  approveExtension(id: number): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/extension/approve`, {});
  }

  rejectExtension(id: number, reason?: string): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/extension/reject`, { reason });
  }

  downloadExhibitionCertificate(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/exhibition-certificate`, { responseType: 'blob' });
  }

  getReviews(id: number): Observable<LoanReviews> {
    return this.http.get<LoanReviews>(`${this.base}/${id}/reviews`);
  }

  createReview(id: number, revieweeId: number, rating: number, comment?: string): Observable<LoanReviews> {
    return this.http.post<LoanReviews>(`${this.base}/${id}/reviews`, { revieweeId, rating, comment });
  }

  /** Resumen público de valoraciones de un usuario. */
  getRating(userId: number): Observable<RatingSummary> {
    return this.http.get<RatingSummary>(`${environment.apiBaseUrl}/users/${userId}/rating`);
  }

  /* ===== Coste estimado y supervisión remota ===== */

  getCostEstimate(id: number): Observable<CostEstimate> {
    return this.http.get<CostEstimate>(`${this.base}/${id}/cost-estimate`);
  }

  /** Estimación previa a la solicitud (fundación con sesión). */
  estimateForRequest(artworkId: number, exhibitionId: number, transportCompanyId?: number): Observable<CostEstimate> {
    const params: Record<string, string> = { exhibitionId: String(exhibitionId) };
    if (transportCompanyId) params['transportCompanyId'] = String(transportCompanyId);
    return this.http.get<CostEstimate>(`${environment.apiBaseUrl}/artworks/${artworkId}/cost-estimate`, { params });
  }

  getSupervision(id: number): Observable<SupervisionCheckpoint[]> {
    return this.http.get<SupervisionCheckpoint[]>(`${this.base}/${id}/supervision`);
  }

  addCheckpoint(id: number, stage: SupervisionStage, note: string, photos: File[]): Observable<SupervisionCheckpoint> {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify({ stage, note })], { type: 'application/json' }));
    photos.forEach((p) => form.append('photos', p));
    return this.http.post<SupervisionCheckpoint>(`${this.base}/${id}/supervision`, form);
  }

  approveCheckpoint(id: number, checkpointId: number): Observable<SupervisionCheckpoint> {
    return this.http.post<SupervisionCheckpoint>(`${this.base}/${id}/supervision/${checkpointId}/approve`, {});
  }

  requestCheckpointChanges(id: number, checkpointId: number, reason: string): Observable<SupervisionCheckpoint> {
    return this.http.post<SupervisionCheckpoint>(
      `${this.base}/${id}/supervision/${checkpointId}/request-changes`,
      { reason },
    );
  }

  /* ===== Documentación previa al envío ===== */

  getDocuments(id: number): Observable<LoanDocumentsResponse> {
    return this.http.get<LoanDocumentsResponse>(`${this.base}/${id}/documents`);
  }

  uploadInsurance(id: number, data: InsuranceCertificateRequest, file: File): Observable<LoanDocumentsResponse> {
    return this.http.post<LoanDocumentsResponse>(`${this.base}/${id}/insurance`, this.multipart(data, file));
  }

  approveInsurance(id: number): Observable<LoanDocumentsResponse> {
    return this.http.post<LoanDocumentsResponse>(`${this.base}/${id}/insurance/approve`, {});
  }

  rejectInsurance(id: number, reason: string): Observable<LoanDocumentsResponse> {
    return this.http.post<LoanDocumentsResponse>(`${this.base}/${id}/insurance/reject`, { reason });
  }

  uploadExportPermit(id: number, data: ExportPermitRequest, file: File): Observable<LoanDocumentsResponse> {
    return this.http.post<LoanDocumentsResponse>(`${this.base}/${id}/export-permit`, this.multipart(data, file));
  }

  /** Multipart: parte `data` en JSON + parte `file`. */
  private multipart(data: object, file: File): FormData {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    form.append('file', file);
    return form;
  }

  /** Informes de estado de la obra en recogidas y entregas. */
  getConditionReports(id: number): Observable<ConditionReportResponse[]> {
    return this.http.get<ConditionReportResponse[]>(`${this.base}/${id}/condition-reports`);
  }

  /** Informe en sala antes de desmontar (fundación). Devuelve todos los informes del préstamo. */
  createPreReturnReport(id: number, report: ConditionReportRequest, photos: File[]): Observable<ConditionReportResponse[]> {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(report)], { type: 'application/json' }));
    photos.forEach((p) => form.append('photos', p));
    return this.http.post<ConditionReportResponse[]>(`${this.base}/${id}/condition-reports`, form);
  }

  /** Exposición itinerante: traslado a la siguiente sede con su informe de estado. */
  transferToNextStop(id: number, report: ConditionReportRequest, photos: File[]): Observable<LoanResponse> {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(report)], { type: 'application/json' }));
    photos.forEach((p) => form.append('photos', p));
    return this.http.post<LoanResponse>(`${this.base}/${id}/transfer`, form);
  }

  /** La otra parte firma su conformidad (o desacuerdo) con un informe de estado. */
  signConditionReport(id: number, reportId: number, agree: boolean, comment?: string): Observable<ConditionReportResponse> {
    return this.http.post<ConditionReportResponse>(
      `${this.base}/${id}/condition-reports/${reportId}/sign`, { agree, comment: comment || null });
  }

  /* ===== Acciones del flujo ===== */

  create(body: CreateLoanRequest): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(this.base, body);
  }

  accept(id: number, body: AcceptLoanRequest): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/accept`, body);
  }

  reject(id: number, body: CancelLoanRequest = {}): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(`${this.base}/${id}/reject`, body);
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

  /**
   * Asigna otra empresa de transporte tras el rechazo de un presupuesto. Solo
   * válido cuando el préstamo está en QUOTE_PENDING y la empresa anterior no
   * era obligatoria. El back crea un nuevo Shipment OUTBOUND en REQUESTED.
   */
  reassignTransport(id: number, transportCompanyId: number): Observable<LoanResponse> {
    return this.http.post<LoanResponse>(
      `${this.base}/${id}/reassign-transport`,
      { transportCompanyId },
    );
  }
  // El cierre del ciclo (RETURNED) se dispara automáticamente cuando el
  // shipment de retorno se marca DELIVERED. No hay endpoint manual.

  /* ===== Contrato PDF ===== */

  downloadContract(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/contract`, { responseType: 'blob' });
  }
}
