import { CompatibilityIssue, CompatibilityReport, CompatibilityResult } from './facility.models';
import { LoanStatus } from './loan.models';

export interface ExhibitionRequest {
  title: string;
  description?: string;
  venueName: string;
  venueCity: string;
  /** País de la sede (ISO de dos letras). */
  venueCountry?: string;
  /** ISO yyyy-MM-dd */
  openingDate: string;
  closingDate: string;
  /** Sedes siguientes si la exposición es itinerante, en orden de gira. */
  stops?: ExhibitionStopInput[];
}

export interface ExhibitionStopInput {
  hostInstitution: string;
  venueName: string;
  venueCity: string;
  venueCountry?: string;
  openingDate: string;
  closingDate: string;
}

export interface ExhibitionStop extends ExhibitionStopInput {
  /** 1 = primera sede después de la principal. */
  position: number;
}

export interface ExhibitionLoanSummary {
  loanId: number;
  artworkId: number;
  artworkTitle: string;
  artworkArtist: string;
  collectorName: string;
  status: LoanStatus;
  startDate: string;
  endDate: string;
}

export interface ExhibitionResponse extends ExhibitionRequest {
  id: number;
  foundationId: number;
  foundationName: string;
  stops: ExhibitionStop[];
  /** Cierre de la última sede de la gira. */
  tourClosingDate: string;
  /** Solo para la fundación organizadora y la administración. */
  loans?: ExhibitionLoanSummary[] | null;
}

/** Compatibilidad de toda la exposición con las instalaciones de la organizadora. */
export interface ExhibitionCompatibility {
  result: CompatibilityResult;
  /** Problemas de conjunto (obras que no pueden compartir sala, sedes sin informe…). */
  sharedIssues: CompatibilityIssue[];
  /** Condiciones que cumplen a la vez todas las obras. */
  tempMin?: number | null;
  tempMax?: number | null;
  rhMin?: number | null;
  rhMax?: number | null;
  maxLux?: number | null;
  maxLuxArtwork?: string | null;
  courierCount: number;
  facilityReportUpdatedAt?: string | null;
  artworks: { loanId: number; artworkTitle: string; status: LoanStatus; report: CompatibilityReport }[];
}
