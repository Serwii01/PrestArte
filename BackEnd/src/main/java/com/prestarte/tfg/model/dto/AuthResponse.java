package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de autenticación. Devuelve el JWT y los datos mínimos
 * que el frontend necesita para pintar la UI sin volver a llamar al backend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String name;
    private Role role;
    private long expiresInMs;
}
