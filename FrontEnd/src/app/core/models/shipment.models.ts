import { Condition } from './artwork.models';

export type ShipmentStatus =
  | 'REQUESTED'
  | 'QUOTED'
  | 'REJECTED'
  | 'APPROVED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'DELIVERED';

export type ShipmentDirection = 'OUTBOUND' | 'RETURN';

export interface ShipmentResponse {
  id: number;
  loanRequestId?: number;
  transportCompanyId?: number;
  trackingNumber: string;
  status: ShipmentStatus;
  direction?: ShipmentDirection;
  transportCompanyName: string;
  artworkTitle: string;
  price?: number;
  /** Precio comprometido para la devolución (en el envío de ida). */
  returnPrice?: number | null;
  insuranceCost?: number;
  insuranceValue?: number;
  insurancePolicy?: string;
  priceAccepted: boolean;
  receivedBy?: string;
  /** Daños señalados sobre las fotos; photoIndex = posición en la lista de fotos. */
  damageMarks?: DamageMarkInput[];
  notes?: string;
  deliveryDate?: string;
  createdAt?: string;
  startDate?: string;
  endDate?: string;
}

export interface ProposeQuoteRequest {
  price: number;
  returnPrice: number;
  insuranceCost: number;
  insurancePolicy?: string;
}

/**
 * Informe de estado que acompaña a la recogida y a la entrega. Las fotos
 * viajan aparte en la petición multipart.
 */
export interface ConditionReportRequest {
  condition: Condition;
  damageReported: boolean;
  notes?: string;
  /** Solo en la entrega. */
  receivedBy?: string;
  /** Daños señalados sobre las fotos; photoIndex = posición en la lista de fotos. */
  damageMarks?: DamageMarkInput[];
}

export interface DamageMarkInput {
  photoIndex: number;
  /** Posición en % (0-100) sobre la foto. */
  x: number;
  y: number;
  note?: string;
}

export interface DamageMark {
  photoId: string;
  x: number;
  y: number;
  note?: string | null;
}

export type ConditionReportStage = 'PICKUP' | 'DELIVERY' | 'PRE_RETURN' | 'TRANSFER';

export interface ConditionReportResponse {
  id: number;
  shipmentId: number;
  direction: ShipmentDirection;
  stage: ConditionReportStage;
  condition: Condition;
  damageReported: boolean;
  notes?: string | null;
  authorName: string;
  authorRole: string;
  /** UUIDs de las fotos (requieren sesión: usar el pipe secureFile). */
  photoIds: string[];
  marks: DamageMark[];
  /** Rol que debe firmar la conformidad. */
  counterpartRole: string;
  /** True si el usuario actual debe firmar y aún no lo ha hecho. */
  canSign: boolean;
  acknowledgedByName?: string | null;
  acknowledgedAt?: string | null;
  disputed?: boolean | null;
  acknowledgementComment?: string | null;
  createdAt: string;
}

export const SHIPMENT_STATUS_LABEL: Record<ShipmentStatus, string> = {
  REQUESTED: 'Asignado',
  QUOTED: 'Presupuesto enviado',
  REJECTED: 'Rechazado',
  APPROVED: 'Aprobado',
  PICKED_UP: 'Recogido',
  IN_TRANSIT: 'En tránsito',
  DELIVERED: 'Entregado',
};
