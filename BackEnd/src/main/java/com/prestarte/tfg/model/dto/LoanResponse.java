package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class LoanResponse {
    private Long id;
    private String artworkTitle;
    private String foundationName;
    private LocalDate startDate; // Coincide con la entidad
    private LocalDate endDate;   // Coincide con la entidad
    private String status;
}