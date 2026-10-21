package com.prestarte.tfg.service;

/**
 * Versión vigente de los textos legales (privacidad, términos de uso,
 * cookies). Se guarda con cada aceptación para saber qué versión aceptó
 * cada usuario. Debe coincidir con la que muestran las páginas legales
 * del frontend; si los textos cambian de forma relevante, sube la versión.
 */
public final class LegalTerms {

    public static final String VERSION = "2026-10";

    private LegalTerms() {
    }
}
