export interface CostLine {
  concept: string;
  /** Importe (€) o kg CO₂e; null si no se puede estimar. */
  amount: number | null;
  note?: string | null;
}

/** Estimación orientativa del coste y la huella de carbono de un préstamo. */
export interface CostEstimate {
  available: boolean;
  message?: string | null;
  origin?: string;
  destination?: string;
  distanceKm?: number;
  transportMode?: 'ROAD' | 'AIR';
  /** Empresa cuya tarifa se ha usado, o null si es la tarifa de referencia. */
  tariffSource?: string | null;
  costLines?: CostLine[];
  totalCost?: number;
  co2Lines?: CostLine[];
  totalCo2Kg?: number;
  co2SavedWithVirtualCourier?: number | null;
  assumptions?: string[];
}

export type SupervisionStage =
  | 'PACKING'
  | 'LOADING'
  | 'UNLOADING'
  | 'INSTALLATION'
  | 'DEINSTALLATION'
  | 'REPACKING';

export const SUPERVISION_STAGE_LABEL: Record<SupervisionStage, string> = {
  PACKING: 'Embalaje (ida)',
  LOADING: 'Carga',
  UNLOADING: 'Descarga',
  INSTALLATION: 'Montaje',
  DEINSTALLATION: 'Desmontaje',
  REPACKING: 'Embalaje (vuelta)',
};

export interface SupervisionCheckpoint {
  id: number;
  stage: SupervisionStage;
  note?: string | null;
  photoIds: string[];
  authorName: string;
  authorRole: string;
  createdAt: string;
  status: 'PENDING' | 'APPROVED' | 'CHANGES_REQUESTED';
  reviewNote?: string | null;
  reviewedAt?: string | null;
}
