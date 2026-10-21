package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Compatibilidad de una exposición completa con las instalaciones de la
 * institución: cada obra por separado y, además, lo que exigen todas
 * juntas si comparten sala (rangos de clima comunes, luz más restrictiva).
 */
@Data
@Builder
public class ExhibitionCompatibility {

    /** Peor resultado entre las obras y las comprobaciones de conjunto. */
    private CompatibilityReport.Result result;

    /** Problemas de conjunto: obras que no pueden compartir sala, sedes sin informe… */
    private List<CompatibilityReport.Issue> sharedIssues;

    /** Condiciones que cumplen a la vez todas las obras (nulo si no exigen nada o no hay rango común). */
    private Double tempMin;
    private Double tempMax;
    private Double rhMin;
    private Double rhMax;
    /** Luz máxima que admite la obra más delicada. */
    private Integer maxLux;
    private String maxLuxArtwork;

    /** Obras que exigen correo presencial en montaje y desmontaje. */
    private int courierCount;

    private LocalDateTime facilityReportUpdatedAt;

    private List<ArtworkResult> artworks;

    @Data
    @Builder
    public static class ArtworkResult {
        private Long loanId;
        private String artworkTitle;
        private String status;
        private CompatibilityReport report;
    }
}
