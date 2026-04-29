package com.prestarte.tfg.model.dto;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String email;
    private String password;
    private String name;
    private String phone;
    private String taxId; // DNI, CIF o LEI
    private String role;  // "COLLECTOR", "FOUNDATION", "TRANSPORT_COMPANY"
}