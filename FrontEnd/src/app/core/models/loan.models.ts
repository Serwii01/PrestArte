export type LoanStatus =
  | 'REQUESTED'
  | 'ACCEPTED'
  | 'QUOTE_PENDING'
  | 'QUOTE_PROPOSED'
  | 'PAID'
  | 'READY_FOR_PICKUP'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'ON_LOAN'
  | 'RETURNING'
  | 'RETURNED'
  | 'REJECTED'
  | 'CANCELLED';

export interface LoanResponse {
  id: number;
  artworkId: number;
  artworkTitle: string;
  artworkArtist: string;
  collectorId: number;
  collectorName: string;
  foundationId: number;
  foundationName: string;
  startDate: string;
  endDate: string;
  agreedConditions?: string;
  status: LoanStatus;
  transportCompanyMandatory: boolean;
  /** Id del Shipment OUTBOUND vinculado, o null si aún no se ha creado. */
  shipmentId?: number | null;
  cancelledAt?: string | null;
  cancellationReason?: string | null;
}

export interface CreateLoanRequest {
  artworkId: number;
  foundationId: number;
  startDate: string; // ISO yyyy-MM-dd
  endDate: string;
  agreedConditions?: string;
}

export interface AcceptLoanRequest {
  transportCompanyId: number;
  transportCompanyMandatory: boolean;
}

export interface CancelLoanRequest {
  reason?: string;
}

/** Etiquetas en español para mostrar al usuario. */
export const LOAN_STATUS_LABEL: Record<LoanStatus, string> = {
  REQUESTED: 'Solicitado',
  ACCEPTED: 'Aceptado',
  QUOTE_PENDING: 'Esperando presupuesto',
  QUOTE_PROPOSED: 'Presupuesto propuesto',
  PAID: 'Pagado',
  READY_FOR_PICKUP: 'Listo para recoger',
  IN_TRANSIT: 'En tránsito',
  DELIVERED: 'Entregado',
  ON_LOAN: 'En préstamo',
  RETURNING: 'En devolución',
  RETURNED: 'Devuelto',
  REJECTED: 'Rechazado',
  CANCELLED: 'Cancelado',
};
