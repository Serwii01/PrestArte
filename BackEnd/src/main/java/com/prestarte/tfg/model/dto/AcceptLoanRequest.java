package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload utilizado por el coleccionista para aceptar una solicitud
 * de préstamo.
 *
 * Indica la empresa de transporte elegida y si esa elección debe
 * considerarse obligatoria. Si lo es, un rechazo posterior del
 * presupuesto cancelará el préstamo en lugar de permitir reasignar a
 * otra empresa.
 */
@Data
public class AcceptLoanRequest {

    @NotNull(message = "Debes elegir una empresa de transporte")
    private Long transportCompanyId;

    /** Marca la empresa elegida como obligatoria para este préstamo. */
    private boolean transportCompanyMandatory;
}
