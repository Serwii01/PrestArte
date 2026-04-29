package com.prestarte.tfg.model.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLoanRequest {
    private Long artworkId;
    private Long foundationId;
    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;
    private String additionalConditions;
}