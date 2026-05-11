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
  insuranceCost?: number;
  insuranceValue?: number;
  insurancePolicy?: string;
  priceAccepted: boolean;
  receivedBy?: string;
  notes?: string;
  deliveryDate?: string;
  createdAt?: string;
  startDate?: string;
  endDate?: string;
}

export interface ProposeQuoteRequest {
  price: number;
  insuranceCost: number;
  insurancePolicy?: string;
}

export interface ConfirmReceiptRequest {
  receivedBy: string;
  notes?: string;
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
