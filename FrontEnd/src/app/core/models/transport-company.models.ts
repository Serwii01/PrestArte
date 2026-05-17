export interface TransportCompanyResponse {
  id: number;
  email: string;
  name: string;
  phone?: string;
  taxId?: string;
  companyName: string;
  coverageArea?: string;
  contactEmail?: string;
}

/** Perfil público devuelto por GET /api/transport-companies. */
export interface TransportCompanyProfile {
  id: number;
  companyName: string;
  contactEmail?: string;
  website?: string;
  description?: string;
  specialties?: string;
  locations?: string;
  coverageArea?: string;
}

/** Body para PUT /api/transport-companies/{id}/profile. */
export interface UpdateTransportCompanyRequest {
  companyName?: string;
  contactEmail?: string;
  website?: string;
  description?: string;
  specialties?: string;
  locations?: string;
  coverageArea?: string;
}
