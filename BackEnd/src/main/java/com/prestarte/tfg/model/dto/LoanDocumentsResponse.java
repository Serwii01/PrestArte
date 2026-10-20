package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado de la documentación obligatoria antes de que la obra salga:
 * seguro y, si hace falta, permiso de exportación.
 */
@Data
@Builder
public class LoanDocumentsResponse {

    /** Valor asegurado acordado al aceptar el préstamo. */
    private Double agreedValue;

    private Insurance insurance;

    /** Si la obra necesita permiso de exportación temporal para este préstamo. */
    private boolean permitRequired;
    /** Explicación legible de por qué hace falta (o no) el permiso. */
    private String permitReason;
    private Permit permit;

    /** True si la obra puede salir: seguro aprobado y permiso, si hace falta. */
    private boolean readyForShipment;
    /** Lo que falta para poder enviar la obra. */
    private List<String> missing;

    @Data
    @Builder
    public static class Insurance {
        private Long id;
        private String coverageType;
        private String insurer;
        private String policyNumber;
        private Double insuredAmount;
        private LocalDate coverageStart;
        private LocalDate coverageEnd;
        private String documentFileId;
        private String documentFileName;
        private String status;
        private String reviewNote;
        private String uploadedByName;
        private LocalDateTime createdAt;
        private LocalDateTime reviewedAt;
    }

    @Data
    @Builder
    public static class Permit {
        private Long id;
        private String resolutionNumber;
        private String issuedBy;
        private LocalDate issueDate;
        private LocalDate returnDeadline;
        private String documentFileId;
        private String documentFileName;
        private String uploadedByName;
        private LocalDateTime createdAt;
    }
}
