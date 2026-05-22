package com.prestarte.tfg.model.dto;

import lombok.Data;

/**
 * Payload utilizado para confirmar la recepción de un envío.
 *
 * Recoge el nombre de la persona que firma la entrega y unas notas
 * libres con cualquier observación sobre el estado de la obra en el
 * momento de la recepción.
 */
@Data
public class ConfirmReceiptRequest {
    private String receivedBy;
    private String notes;
}
