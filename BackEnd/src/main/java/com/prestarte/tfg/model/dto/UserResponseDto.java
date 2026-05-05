package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de salida para User. Nunca expone password ni archivos sensibles.
 * Devuelto por endpoints de admin, perfil propio y registro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private Role role;
    private UserStatus status;
    private boolean enabled;
    private String taxId;
}
