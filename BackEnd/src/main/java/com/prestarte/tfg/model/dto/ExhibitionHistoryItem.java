package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/** Exposición en la que ha participado una obra (historial público). */
@Data
@Builder
public class ExhibitionHistoryItem {
    private String title;
    private String venueName;
    private String venueCity;
    private String foundationName;
    private LocalDate openingDate;
    private LocalDate closingDate;
}
