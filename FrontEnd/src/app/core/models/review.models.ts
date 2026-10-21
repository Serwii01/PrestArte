export interface ReviewItem {
  id: number;
  reviewerName: string;
  reviewerRole: string;
  revieweeName: string;
  revieweeRole: string;
  rating: number;
  comment?: string | null;
  createdAt: string;
}

export interface Reviewable {
  userId: number;
  name: string;
  role: string;
}

export interface LoanReviews {
  reviews: ReviewItem[];
  /** Partes a las que el usuario con sesión todavía puede valorar. */
  pending: Reviewable[];
  canReview: boolean;
}

export interface RatingSummary {
  average: number | null;
  count: number;
  recent: { rating: number; comment: string; createdAt: string }[];
}
