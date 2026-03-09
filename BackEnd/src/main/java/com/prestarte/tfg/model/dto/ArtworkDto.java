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
    private String condition;           // "EXCELENTE"
    private String description;
    private String loanConditions;      // JSON string
    private String collectorName;       // "Juan Pérez"
    private List<FileDto> files;        // URLs fotos
    private LocalDateTime createdAt;
}
