package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.Artwork;

/**
 * Integración automática con una base de obras robadas o expoliadas.
 *
 * Las bases públicas no ofrecen API abierta (Interpol pide registro; Art
 * Loss Register cobra por certificado), así que de serie la revisión es
 * manual. Quien contrate un acceso con API solo tiene que implementar esta
 * interfaz como bean de Spring: {@link StolenArtRegistries} la ejecutará
 * al revisar cada obra y mostrará el resultado junto a las demás bases.
 */
public interface StolenArtRegistryClient {

    /** Código de la base en {@link StolenArtRegistries} (p. ej. "ALR"). */
    String code();

    Result search(Artwork artwork);

    enum Outcome { NO_MATCH, POSSIBLE_MATCH, ERROR }

    /** Resultado de una búsqueda; {@code reference} es el nº de certificado o de búsqueda. */
    record Result(Outcome outcome, String detail, String reference) {
    }
}
