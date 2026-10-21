export type AcquisitionMethod =
  | 'ARTIST'
  | 'PURCHASE'
  | 'AUCTION'
  | 'INHERITANCE'
  | 'GIFT'
  | 'COMMISSION'
  | 'EXCHANGE'
  | 'UNKNOWN';

export const ACQUISITION_LABEL: Record<AcquisitionMethod, string> = {
  ARTIST: 'El propio artista',
  PURCHASE: 'Compra',
  AUCTION: 'Subasta',
  INHERITANCE: 'Herencia',
  GIFT: 'Donación o regalo',
  COMMISSION: 'Encargo',
  EXCHANGE: 'Intercambio',
  UNKNOWN: 'Desconocido',
};

export type DueDiligenceStatus = 'PENDING' | 'CLEAR' | 'FLAGGED';

/** Mismo texto que Artwork.TITLE_DECLARATION en el backend. */
export const TITLE_DECLARATION =
  'Declaro que soy el legítimo propietario de esta obra o que estoy autorizado por él para ' +
  'prestarla; que, según mi conocimiento, no ha sido robada, expoliada ni exportada ' +
  'ilícitamente; y que no conozco reclamaciones de terceros sobre ella. Me comprometo a ' +
  'comunicar a PrestArte cualquier reclamación que pueda surgir.';

export interface ProvenanceEntryRequest {
  owner: string;
  fromYear: number;
  toYear?: number | null;
  acquisitionMethod: AcquisitionMethod;
  notes?: string;
}

export interface ProvenanceEntry extends ProvenanceEntryRequest {
  id: number;
  documentFileId?: string | null;
  documentFileName?: string | null;
  hasDocument: boolean;
}

export interface ProvenanceGap {
  fromYear: number;
  toYear: number;
  /** Cae en 1933–1945 (expolio nazi). */
  critical: boolean;
  message: string;
}

export interface ProvenanceResponse {
  artworkId: number;
  artworkTitle: string;
  artworkYear?: number | null;
  titleDeclarationText: string;
  titleDeclaredAt?: string | null;
  entries: ProvenanceEntry[];
  gaps: ProvenanceGap[];
  dueDiligenceStatus: DueDiligenceStatus;
  dueDiligence?: {
    result: DueDiligenceStatus;
    sources: string;
    reference?: string | null;
    notes?: string | null;
    checkedByName: string;
    checkedAt: string;
  } | null;
  editable: boolean;
}

export interface DueDiligenceRequest {
  result: 'CLEAR' | 'FLAGGED';
  sources: string;
  reference?: string;
  notes?: string;
}

export interface DueDiligencePendingItem {
  artworkId: number;
  title: string;
  artist: string;
  year?: number | null;
  collectorName: string;
  titleDeclaredAt?: string | null;
  provenanceEntries: number;
  provenanceGaps: number;
  criticalGap: boolean;
  createdAt?: string | null;
}
