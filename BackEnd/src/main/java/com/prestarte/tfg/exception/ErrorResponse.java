package com.prestarte.tfg.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura uniforme de error que devuelve la API.
 * Garantiza que el frontend siempre recibe el mismo contrato ante un fallo.
 */
@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /** Errores de validación campo→mensaje. Null si no aplica. */
    private Map<String, String> fieldErrors;
}
