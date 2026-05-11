package com.prestarte.tfg.model.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkDto {

    private Long id;
    private String title;
    private String artist;
    private Integer year;
    private Double widthCm;
    private Double heightCm;
    private Double depthCm;
    private String condition;           // EXCELLENT, GOOD, FAIR, POOR, DAMAGED
    private String description;
    private Double estimatedValue;
    private String loanConditions;
    private String location;
    private Long collectorId;
    private String collectorName;
    private Long preferredTransportCompanyId;
    private String preferredTransportCompanyName;
    private boolean preferredTransportMandatory;
    private List<FileDto> files;
    private LocalDateTime createdAt;
}
