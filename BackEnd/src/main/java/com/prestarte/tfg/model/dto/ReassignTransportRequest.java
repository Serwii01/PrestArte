package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload utilizado para reasignar la empresa de transporte de un
 * préstamo cuando el museo ha rechazado el presupuesto anterior.
 *
 * Indica la nueva empresa que se encargará de elaborar el
 * presupuesto, evitando tener que crear un préstamo nuevo desde
 * cero.
 */
@Data
public class ReassignTransportRequest {

    @NotNull(message = "Debes elegir una nueva empresa de transporte")
    private Long transportCompanyId;
}
