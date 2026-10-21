export type Condition = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'DAMAGED';

export type ArtworkFileType =
  | 'IMAGE_MAIN'
  | 'IMAGE_DETAIL'
  | 'IMAGE_SIDE'
  | 'IMAGE_BACK'
  | 'DOCUMENT';

export interface FileDto {
  /** UUID del DBFile, sirve para GET /api/files/{id}. */
  id: string;
  /** Id interno del ArtworkFile, necesario para borrar el adjunto. */
  artworkFileId?: number;
  fileName: string;
  fileType: string;
  fileSize?: number;
  type?: ArtworkFileType;
  description?: string;
  confidential?: boolean;
}

export type ArtworkVisibility = 'PUBLIC' | 'MEMBERS';

export type CourierMode = 'NONE' | 'VIRTUAL' | 'IN_PERSON';

/** Protección patrimonial (Ley 16/1985). */
export type HeritageStatus = 'NONE' | 'INVENTORIED' | 'BIC';

/**
 * Requisitos estructurados de préstamo de una obra. Nulo = sin requisito.
 * Se comparan con el informe de instalaciones de la fundación.
 */
export interface LoanRequirements {
  tempMin?: number | null;
  tempMax?: number | null;
  rhMin?: number | null;
  rhMax?: number | null;
  maxLux?: number | null;
  climateControl?: boolean | null;
  displayCase?: boolean | null;
  guards24h?: boolean | null;
  cctv?: boolean | null;
  intrusionAlarm?: boolean | null;
  fireDetection?: boolean | null;
  /** Días mínimos entre la solicitud y el inicio del préstamo. */
  minNoticeDays?: number | null;
  /** Supervisión del prestador: ninguna, correo virtual o correo presencial. */
  courierMode?: CourierMode | null;
}

export interface ArtworkResponse {
  id: number;
  title: string;
  artist: string;
  year?: number;
  widthCm?: number;
  heightCm?: number;
  depthCm?: number;
  condition: string;
  description?: string;
  /** Valor exacto: solo llega al dueño y a la administración. */
  estimatedValue?: number | null;
  /** Horquilla de valor para usuarios verificados (p. ej. "100.000 – 250.000 €"). */
  estimatedValueRange?: string | null;
  loanConditions?: string;
  requirements?: LoanRequirements | null;
  /** Ciudad o provincia (pública). */
  location?: string;
  /** Dirección exacta: dueño, admin y transportista con envío en curso. */
  pickupAddress?: string | null;
  /** Nulo para terceros si la obra es anónima. */
  collectorId?: number | null;
  /** "Colección particular" para terceros si la obra es anónima. */
  collectorName?: string;
  /** Ajustes de privacidad: solo llegan al dueño y a la administración. */
  showOwnerName?: boolean | null;
  visibility?: ArtworkVisibility | null;
  heritageStatus?: HeritageStatus | null;
  /** Si el coleccionista ha firmado la declaración de titularidad. */
  titleDeclared?: boolean;
  /** PENDING, CLEAR (procedencia verificada) o FLAGGED. */
  dueDiligenceStatus?: 'PENDING' | 'CLEAR' | 'FLAGGED';
  /** Condiciones del prestador. */
  creditLine?: string | null;
  imageRights?: ImageRights | null;
  loanFee?: number | null;
  preferredTransportCompanyId?: number | null;
  preferredTransportCompanyName?: string | null;
  preferredTransportMandatory?: boolean;
  availableForLoan?: boolean;
  files?: FileDto[];
  createdAt?: string;
}

export interface UpdateArtworkRequest {
  title?: string;
  artist?: string;
  year?: number;
  widthCm?: number;
  heightCm?: number;
  depthCm?: number;
  condition?: Condition;
  description?: string;
  estimatedValue?: number;
  loanConditions?: string;
  requirements?: LoanRequirements | null;
  location?: string;
  pickupAddress?: string;
  showOwnerName?: boolean;
  visibility?: ArtworkVisibility;
  heritageStatus?: HeritageStatus;
  /** Firma la declaración de titularidad. */
  titleDeclared?: boolean;
  creditLine?: string;
  imageRights?: ImageRights;
  loanFee?: number;
  preferredTransportCompanyId?: number | null;
  preferredTransportMandatory?: boolean;
}

export interface CreateArtworkRequest {
  title: string;
  artist: string;
  year?: number;
  widthCm?: number;
  heightCm?: number;
  depthCm?: number;
  condition: Condition;
  description?: string;
  estimatedValue: number;
  loanConditions?: string;
  requirements?: LoanRequirements | null;
  location?: string;
  pickupAddress?: string;
  showOwnerName?: boolean;
  visibility?: ArtworkVisibility;
  heritageStatus?: HeritageStatus;
  /** Firma la declaración de titularidad. */
  titleDeclared?: boolean;
  creditLine?: string;
  imageRights?: ImageRights;
  loanFee?: number;
  preferredTransportCompanyId?: number | null;
  preferredTransportMandatory?: boolean;
  collectorId: number;
}

export const CONDITION_LABEL: Record<Condition, string> = {
  EXCELLENT: 'Excelente',
  GOOD: 'Bueno',
  FAIR: 'Regular',
  POOR: 'Defectuoso',
  DAMAGED: 'Dañado',
};

export const CONDITION_OPTIONS: Array<{ value: Condition; label: string; description: string }> = [
  { value: 'EXCELLENT', label: 'Excelente', description: 'Como nueva, sin defectos visibles' },
  { value: 'GOOD', label: 'Bueno', description: 'Marcas mínimas del paso del tiempo' },
  { value: 'FAIR', label: 'Regular', description: 'Restauraciones puntuales documentadas' },
  { value: 'POOR', label: 'Defectuoso', description: 'Daños visibles que requieren atención' },
  { value: 'DAMAGED', label: 'Dañado', description: 'Necesita restauración antes de exponerse' },
];

/** ¿Es una imagen "principal de la obra" (no documento)? */
export function isArtworkImage(f: FileDto): boolean {
  if (f.type) return f.type !== 'DOCUMENT';
  return (f.fileType ?? '').startsWith('image/');
}

/** ¿Es un documento adjunto (seguro, certificado, etc.)? */
export function isArtworkDocument(f: FileDto): boolean {
  return f.type === 'DOCUMENT';
}

/** Usos autorizados de la imagen de la obra durante el préstamo. */
export interface ImageRights {
  catalog?: boolean | null;
  press?: boolean | null;
  socialMedia?: boolean | null;
  merchandise?: boolean | null;
  visitorPhotos?: boolean | null;
}

export const IMAGE_RIGHTS_LABEL: Record<keyof ImageRights, string> = {
  catalog: 'Catálogo de la exposición',
  press: 'Prensa',
  socialMedia: 'Web y redes sociales',
  merchandise: 'Productos de tienda',
  visitorPhotos: 'Fotos del público (sin flash)',
};

/** Exposición en la que ha participado una obra. */
export interface ExhibitionHistoryItem {
  title: string;
  venueName: string;
  venueCity: string;
  foundationName: string;
  openingDate: string;
  closingDate: string;
}

/** Periodo en que la obra ya está reservada. */
export interface BusyPeriod {
  startDate: string;
  endDate: string;
}

export interface ArtworkSearchParams {
  q?: string;
  artist?: string;
  yearFrom?: number | null;
  yearTo?: number | null;
  city?: string;
  condition?: string;
  verifiedOnly?: boolean;
  lendableOnly?: boolean;
  availableFrom?: string;
  availableTo?: string;
  sort?: 'recent' | 'title' | 'artist' | 'year';
  page?: number;
  size?: number;
}

export interface ArtworkPage {
  items: ArtworkResponse[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}
