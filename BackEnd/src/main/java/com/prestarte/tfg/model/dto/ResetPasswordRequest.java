package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
    private String newPassword;
}
