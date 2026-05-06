package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body para POST /api/loan-requests/{id}/accept.
 * El coleccionista acepta el préstamo eligiendo una empresa de transporte y
 * decide si esa empresa es obligatoria (no se puede sustituir si el museo
 * rechaza el presupuesto).
 */
@Data
public class AcceptLoanRequest {

    @NotNull(message = "Debes elegir una empresa de transporte")
    private Long transportCompanyId;

    /** Si true, el museo no podrá pedir presupuesto a otra empresa. */
    private boolean transportCompanyMandatory;
}
