export type Condition = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'DAMAGED';

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
  files?: FileDto[];
  createdAt?: string;
}

export interface FileDto {
  id: string;
  fileName: string;
  fileType: string;
  url?: string;
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
