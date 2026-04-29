package com.prestarte.tfg.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateArtworkRequest {
    private String title;
    private String artist;
    private Integer year;
    private Double widthCm;
    private Double heightCm;
    private Double depthCm;
    private String condition; // Ejemplo: "EXCELENTE"
    private String description;
    private Long collectorId; // Muy importante para asociar la obra al usuario
}