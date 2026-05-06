package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body para POST /api/loan-requests/{id}/cancel.
 */
@Data
public class CancelLoanRequest {

    @Size(max = 500, message = "La razón no puede superar 500 caracteres")
    private String reason;
}
