package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Titularidad y procedencia de una obra: declaración del coleccionista,
 * historial de propietarios, huecos detectados y diligencia debida.
 */
@Data
@Builder
public class ProvenanceResponse {

    private Long artworkId;
    private String artworkTitle;
    private Integer artworkYear;

    /** Texto de la declaración y cuándo se firmó (null si no se ha firmado). */
    private String titleDeclarationText;
    private LocalDateTime titleDeclaredAt;

    private List<Entry> entries;
    private List<Gap> gaps;

    /** PENDING, CLEAR o FLAGGED. */
    private String dueDiligenceStatus;
    private DueDiligence dueDiligence;

    /** Si quien mira puede añadir o borrar eslabones (dueño o admin). */
    private boolean editable;

    @Data
    @Builder
    public static class Entry {
        private Long id;
        private String owner;
        private Integer fromYear;
        private Integer toYear;
        private String acquisitionMethod;
        private String notes;
        /** Justificante: solo para dueño, admin y fundaciones que han pedido la obra. */
        private String documentFileId;
        private String documentFileName;
        /** True si hay justificante aunque quien mira no pueda verlo. */
        private boolean hasDocument;
    }

    @Data
    @Builder
    public static class Gap {
        private Integer fromYear;
        private Integer toYear;
        /** True si el hueco cae en 1933–1945 (periodo de expolio nazi). */
        private boolean critical;
        private String message;
    }

    @Data
    @Builder
    public static class DueDiligence {
        private String result;
        private String sources;
        private String reference;
        /** Notas internas: solo para la administración. */
        private String notes;
        private String checkedByName;
        private LocalDateTime checkedAt;
    }
}
