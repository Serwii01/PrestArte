package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload utilizado para cancelar un préstamo.
 *
 * El motivo de cancelación es opcional y se guarda en el préstamo
 * como referencia para el histórico.
 */
@Data
public class CancelLoanRequest {

    @Size(max = 500, message = "La razón no puede superar 500 caracteres")
    private String reason;
}
