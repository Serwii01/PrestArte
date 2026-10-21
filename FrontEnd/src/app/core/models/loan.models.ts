import type { ExhibitionStop } from './exhibition.models';

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
  /** Exposición para la que se pide la obra (nula en préstamos antiguos). */
  exhibitionId?: number | null;
  exhibitionTitle?: string | null;
  exhibitionVenue?: string | null;
  exhibitionOpeningDate?: string | null;
  exhibitionClosingDate?: string | null;
  /** Sedes siguientes si la exposición es itinerante. */
  exhibitionStops?: ExhibitionStop[];
  /** Sede donde está la obra: 0 = principal, 1.. = siguientes. */
  currentStop?: number;
  startDate: string;
  endDate: string;
  agreedConditions?: string;
  status: LoanStatus;
  transportCompanyMandatory: boolean;
  /** Id del Shipment OUTBOUND vinculado, o null si aún no se ha creado. */
  shipmentId?: number | null;
  /** Id del Shipment RETURN vinculado (solo a partir de RETURNING). */
  returnShipmentId?: number | null;
  cancelledAt?: string | null;
  cancellationReason?: string | null;
  rejectionReason?: string | null;
  /** Valor asegurado acordado al aceptar (€). */
  agreedValue?: number | null;
  /** Ampliación pendiente de aprobar por el coleccionista. */
  requestedEndDate?: string | null;
  extensionReason?: string | null;
}

/** Entrada del historial del préstamo. */
export interface LoanEventResponse {
  id: number;
  fromStatus?: LoanStatus | null;
  toStatus?: LoanStatus | null;
  message: string;
  /** null cuando el hito lo generó el sistema. */
  actorName?: string | null;
  actorRole?: string | null;
  createdAt: string;
}

export interface CreateLoanRequest {
  artworkId: number;
  foundationId: number;
  exhibitionId: number;
  startDate: string; // ISO yyyy-MM-dd
  endDate: string;
  agreedConditions?: string;
}

export interface AcceptLoanRequest {
  transportCompanyId: number;
  transportCompanyMandatory: boolean;
  /** Valor asegurado acordado (€). Si no se indica, el valor estimado de la obra. */
  agreedValue?: number;
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
  PAID: 'Presupuesto aprobado',
  READY_FOR_PICKUP: 'Listo para recoger',
  IN_TRANSIT: 'En tránsito',
  DELIVERED: 'Entregado',
  ON_LOAN: 'En préstamo',
  RETURNING: 'En devolución',
  RETURNED: 'Devuelto',
  REJECTED: 'Rechazado',
  CANCELLED: 'Cancelado',
};

export type CoverageType = 'BORROWER_POLICY' | 'LENDER_POLICY' | 'STATE_INDEMNITY';
export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export const COVERAGE_LABEL: Record<CoverageType, string> = {
  BORROWER_POLICY: 'Póliza de la institución',
  LENDER_POLICY: 'Póliza del prestador',
  STATE_INDEMNITY: 'Garantía del Estado',
};

export interface InsuranceCertificateRequest {
  coverageType: CoverageType;
  insurer: string;
  policyNumber: string;
  insuredAmount: number;
  /** ISO yyyy-MM-dd */
  coverageStart: string;
  coverageEnd: string;
}

export interface ExportPermitRequest {
  resolutionNumber: string;
  issuedBy: string;
  issueDate: string;
  returnDeadline: string;
}

/** Estado del seguro y del permiso de exportación antes de que la obra salga. */
export interface LoanDocumentsResponse {
  agreedValue: number;
  insurance?: (InsuranceCertificateRequest & {
    id: number;
    documentFileId: string;
    documentFileName: string;
    status: ReviewStatus;
    reviewNote?: string | null;
    uploadedByName: string;
    createdAt: string;
    reviewedAt?: string | null;
  }) | null;
  permitRequired: boolean;
  permitReason: string;
  permit?: (ExportPermitRequest & {
    id: number;
    documentFileId: string;
    documentFileName: string;
    uploadedByName: string;
    createdAt: string;
  }) | null;
  readyForShipment: boolean;
  missing: string[];
}
