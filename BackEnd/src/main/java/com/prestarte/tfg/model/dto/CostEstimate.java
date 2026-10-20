package com.prestarte.tfg.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Estimación orientativa del coste y la huella de carbono de un préstamo,
 * disponible desde antes de enviar la solicitud.
 */
@Data
@Builder
public class CostEstimate {

    /** False si no se puede calcular (ciudad desconocida, sin exposición...). */
    private boolean available;
    /** Motivo cuando no está disponible. */
    private String message;

    private String origin;
    private String destination;
    /** Distancia por carretera estimada, o en línea recta si es por avión (km). */
    private Double distanceKm;
    /** ROAD o AIR. */
    private String transportMode;
    /** Empresa cuya tarifa se ha usado, o null si es la tarifa de referencia. */
    private String tariffSource;

    private List<Line> costLines;
    /** Suma de las líneas con importe (€). */
    private Double totalCost;

    private List<Line> co2Lines;
    /** Huella total estimada (kg CO₂e). */
    private Double totalCo2Kg;
    /** Huella que se evitaría con correo virtual en vez de presencial (kg CO₂e). */
    private Double co2SavedWithVirtualCourier;

    /** Supuestos usados, para que nadie tome la estimación por un presupuesto. */
    private List<String> assumptions;

    @Data
    @AllArgsConstructor
    public static class Line {
        private String concept;
        /** Importe (€) o kg CO₂e; null si no se puede estimar. */
        private Double amount;
        private String note;
    }
}
