package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta devuelta por el endpoint de inicio de sesión.
 *
 * Incluye el token JWT que el cliente debe utilizar en las
 * peticiones autenticadas y los datos mínimos del usuario que el
 * frontend necesita para pintar la interfaz sin volver a consultar al
 * servidor.
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
