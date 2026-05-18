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
  estimatedValue?: number;
  loanConditions?: string;
  location?: string;
  collectorId?: number;
  collectorName?: string;
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
  location?: string;
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
  location?: string;
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
