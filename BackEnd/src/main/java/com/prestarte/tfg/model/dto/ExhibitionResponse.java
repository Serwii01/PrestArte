package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** Exposición con el resumen de las obras solicitadas para ella. */
@Data
@Builder
public class ExhibitionResponse {
    private Long id;
    private Long foundationId;
    private String foundationName;
    private String title;
    private String description;
    private String venueName;
    private String venueCity;
    private String venueCountry;
    private LocalDate openingDate;
    private LocalDate closingDate;

    /** Sedes siguientes de la gira (vacío si no es itinerante). */
    private List<Stop> stops;
    /** Cierre de la última sede. */
    private LocalDate tourClosingDate;

    /** Solicitudes de la exposición: solo se rellenan para la fundación y la administración. */
    private List<LoanSummary> loans;

    public record Stop(int position, String hostInstitution, String venueName, String venueCity,
                       String venueCountry, LocalDate openingDate, LocalDate closingDate) {
    }

    @Data
    @Builder
    public static class LoanSummary {
        private Long loanId;
        private Long artworkId;
        private String artworkTitle;
        private String artworkArtist;
        private String collectorName;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
