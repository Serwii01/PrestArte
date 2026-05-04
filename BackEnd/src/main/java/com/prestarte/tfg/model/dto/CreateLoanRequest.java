package com.prestarte.tfg.model.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateLoanRequest {
    private Long artworkId;
    private Long foundationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String agreedConditions;
}