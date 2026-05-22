package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload del formulario de cambio de contraseña.
 *
 * Lleva el token de un solo uso recibido por correo y la nueva
 * contraseña elegida por el usuario.
 */
@Data
public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
    private String newPassword;
}
