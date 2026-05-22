package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload del formulario de recuperación de contraseña.
 *
 * Contiene únicamente el correo electrónico del usuario que solicita
 * el enlace de restablecimiento.
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;
}
