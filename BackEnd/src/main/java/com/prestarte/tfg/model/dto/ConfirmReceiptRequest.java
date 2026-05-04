package com.prestarte.tfg.model.dto;

import lombok.Data;

@Data
public class ConfirmReceiptRequest {
    private String receivedBy;
    private String notes;
}