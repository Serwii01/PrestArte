package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Datos del informe de instalaciones que rellena la fundación. El PDF
 * oficial, si lo hay, se sube aparte.
 */
@Data
public class FacilityReportRequest {

    private Boolean climateControlled;

    @DecimalMin(value = "-10", message = "Temperatura fuera de rango")
    @DecimalMax(value = "40", message = "Temperatura fuera de rango")
    private Double tempMin;
    @DecimalMin(value = "-10", message = "Temperatura fuera de rango")
    @DecimalMax(value = "40", message = "Temperatura fuera de rango")
    private Double tempMax;

    @DecimalMin(value = "0", message = "La humedad va de 0 a 100 %")
    @DecimalMax(value = "100", message = "La humedad va de 0 a 100 %")
    private Double rhMin;
    @DecimalMin(value = "0", message = "La humedad va de 0 a 100 %")
    @DecimalMax(value = "100", message = "La humedad va de 0 a 100 %")
    private Double rhMax;

    @PositiveOrZero(message = "Los lux no pueden ser negativos")
    private Integer minAchievableLux;

    private Boolean uvFiltered;
    private Boolean guards24h;
    private Boolean cctv;
    private Boolean intrusionAlarm;
    private Boolean fireDetection;

    @Size(max = 200)
    private String fireSuppression;

    private Boolean displayCasesAvailable;

    @Positive(message = "Las medidas del acceso deben ser positivas")
    private Double accessWidthCm;
    @Positive(message = "Las medidas del acceso deben ser positivas")
    private Double accessHeightCm;

    private Boolean loadingDock;

    @Size(max = 5000)
    private String notes;
}
