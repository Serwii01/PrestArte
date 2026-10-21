/**
 * Datos del titular que aparecen en los textos legales (LSSI y RGPD).
 *
 * ⚠️ Rellena estos valores antes de publicar. Los textos de las páginas
 * legales son una base de partida razonable, pero conviene que los revise
 * un profesional (abogado o consultor de protección de datos).
 */
export const LEGAL_OWNER = {
  name: '[NOMBRE O RAZÓN SOCIAL DEL TITULAR]',
  taxId: '[NIF / CIF]',
  address: '[DOMICILIO]',
  email: '[EMAIL DE CONTACTO]',
  /** Datos registrales si el titular es una sociedad; vacío si es persona física. */
  registry: '',
  domain: '[DOMINIO]',
};

/** Debe coincidir con LegalTerms.VERSION en el backend. */
export const LEGAL_VERSION = '2026-10';
export const LEGAL_UPDATED = '21 de octubre de 2026';
