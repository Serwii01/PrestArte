package com.prestarte.tfg.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de comparar los requisitos de una obra con el informe de
 * instalaciones de una fundación.
 */
@Data
@Builder
public class CompatibilityReport {

    public enum Result {
        /** Cumple todo lo que la obra exige. */
        OK,
        /** No hay incumplimientos claros, pero falta información o está desactualizada. */
        WARNINGS,
        /** Hay requisitos que la institución declara no cumplir. */
        BLOCKERS
    }

    public enum Severity { BLOCKER, WARNING }

    private Result result;
    private List<Issue> issues;
    /** Fecha del informe de instalaciones usado, o null si no existe. */
    private LocalDateTime facilityReportUpdatedAt;

    @Data
    @AllArgsConstructor
    public static class Issue {
        private Severity severity;
        /** Apartado: CLIMATE, LIGHT, SECURITY, ACCESS, NOTICE, REPORT. */
        private String area;
        private String message;
    }
}
