package com.prestarte.tfg.model.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoundationPendingRequestDto {
    private Long loanRequestId;
    private String artworkTitle;
    private String artist;
    private String collectorName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String loanConditions; // Las condiciones que pusiste en el Guernica
    private String status;
}