package com.prestarte.tfg.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura uniforme que utiliza la API para informar de cualquier
 * error.
 *
 * Ofrecer siempre el mismo contrato facilita el tratamiento de errores
 * en el cliente: además del código y la descripción genérica, puede
 * incluirse un mapa con los errores asociados a campos concretos
 * cuando el fallo proviene de una validación.
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

    /** Diccionario campo → mensaje cuando el error proviene de una validación. */
    private Map<String, String> fieldErrors;
}
