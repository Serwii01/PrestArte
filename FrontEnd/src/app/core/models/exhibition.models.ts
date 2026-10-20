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
  /** Solo para la fundación organizadora y la administración. */
  loans?: ExhibitionLoanSummary[] | null;
}
