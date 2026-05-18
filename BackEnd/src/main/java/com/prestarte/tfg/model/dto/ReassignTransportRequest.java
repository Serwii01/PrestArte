package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body para POST /api/loan-requests/{id}/reassign-transport.
 *
 * Lo usa el museo o el coleccionista después de que la empresa actual haya
 * visto su presupuesto rechazado: permite escoger otra empresa que vuelva a
 * presupuestar el transporte sin tener que crear un préstamo nuevo.
 */
@Data
public class ReassignTransportRequest {

    @NotNull(message = "Debes elegir una nueva empresa de transporte")
    private Long transportCompanyId;
}
