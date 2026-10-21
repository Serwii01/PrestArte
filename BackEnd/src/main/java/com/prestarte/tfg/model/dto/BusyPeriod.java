package com.prestarte.tfg.model.dto;

import java.time.LocalDate;

/** Periodo en que una obra ya está comprometida con un préstamo (sin datos de las partes). */
public record BusyPeriod(LocalDate startDate, LocalDate endDate) {
}
