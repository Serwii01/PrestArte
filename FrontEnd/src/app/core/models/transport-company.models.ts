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
  /** Tarifa orientativa: fijo por trayecto (€), €/km y caja a medida (€). */
  tariffBaseFee?: number | null;
  tariffPerKm?: number | null;
  tariffCrate?: number | null;
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
  /** Tarifa orientativa: fijo por trayecto (€), €/km y caja a medida (€). */
  tariffBaseFee?: number | null;
  tariffPerKm?: number | null;
  tariffCrate?: number | null;
}
