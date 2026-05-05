package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 50)
    private String taxId; // DNI, CIF o LEI

    /**
     * Solo se aceptan los roles que se pueden crear vía registro público.
     * ADMIN nunca puede registrarse por esta vía: se crea por seed en el arranque.
     */
    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "COLLECTOR|FOUNDATION|MUSEUM|TRANSPORT",
            message = "El rol debe ser COLLECTOR, FOUNDATION o TRANSPORT")
    private String role;
}
