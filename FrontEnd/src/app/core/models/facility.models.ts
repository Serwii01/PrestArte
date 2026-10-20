/** Datos editables del informe de instalaciones de una fundación. */
export interface FacilityReportRequest {
  climateControlled?: boolean | null;
  tempMin?: number | null;
  tempMax?: number | null;
  rhMin?: number | null;
  rhMax?: number | null;
  minAchievableLux?: number | null;
  uvFiltered?: boolean | null;
  guards24h?: boolean | null;
  cctv?: boolean | null;
  intrusionAlarm?: boolean | null;
  fireDetection?: boolean | null;
  fireSuppression?: string | null;
  displayCasesAvailable?: boolean | null;
  accessWidthCm?: number | null;
  accessHeightCm?: number | null;
  loadingDock?: boolean | null;
  notes?: string | null;
}

export interface FacilityReportResponse extends FacilityReportRequest {
  foundationId: number;
  foundationName: string;
  documentFileId?: string | null;
  documentFileName?: string | null;
  updatedAt: string;
  /** False si tiene más de 12 meses. */
  current: boolean;
}

export type CompatibilityResult = 'OK' | 'WARNINGS' | 'BLOCKERS';
export type CompatibilitySeverity = 'BLOCKER' | 'WARNING';

export interface CompatibilityIssue {
  severity: CompatibilitySeverity;
  area: 'CLIMATE' | 'LIGHT' | 'SECURITY' | 'ACCESS' | 'NOTICE' | 'REPORT' | string;
  message: string;
}

/** Comparación entre los requisitos de una obra y las instalaciones de una fundación. */
export interface CompatibilityReport {
  result: CompatibilityResult;
  issues: CompatibilityIssue[];
  facilityReportUpdatedAt?: string | null;
}
