package com.prestarte.tfg.model.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filtros del catálogo (todos opcionales). */
@Data
public class ArtworkSearchCriteria {
    /** Texto libre: título, artista, descripción o ciudad. */
    private String q;
    private String artist;
    private Integer yearFrom;
    private Integer yearTo;
    private String city;
    /** Estado de conservación (EXCELLENT, GOOD...). */
    private String condition;
    /** Solo obras con la procedencia verificada. */
    private boolean verifiedOnly;
    /** Solo obras que admiten solicitudes ahora mismo. */
    private boolean lendableOnly;
    /** Solo obras libres en todo este intervalo. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableTo;
    /** recent (por defecto), title, artist, year. */
    private String sort;
    private int page = 0;
    private int size = 12;
}
