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
  loanConditions?: string;
  collectorName?: string;
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
  collectorId: number;
}

export const CONDITION_LABEL: Record<Condition, string> = {
  EXCELLENT: 'Excelente',
  GOOD: 'Bueno',
  FAIR: 'Regular',
  POOR: 'Defectuoso',
  DAMAGED: 'Dañado',
};
