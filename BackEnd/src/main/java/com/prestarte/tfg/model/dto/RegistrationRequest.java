package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload de registro. Todos los campos (excepto el rol, que viene
 * forzado por la UI) son obligatorios; el documento KYB viaja aparte
 * como `multipart/form-data` y se valida en {@code UserService}.
 */
@Data
public class RegistrationRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 6, max = 20, message = "El teléfono debe tener al menos 6 caracteres")
    private String phone;

    @NotBlank(message = "El DNI / CIF / LEI es obligatorio")
    @Size(min = 4, max = 50, message = "El DNI / CIF / LEI no es válido")
    private String taxId;

    /**
     * Solo se aceptan los roles que se pueden crear vía registro público.
     * ADMIN nunca puede registrarse por esta vía: se crea por seed en el arranque.
     */
    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "COLLECTOR|FOUNDATION|MUSEUM|TRANSPORT",
            message = "El rol debe ser COLLECTOR, FOUNDATION o TRANSPORT")
    private String role;
}
