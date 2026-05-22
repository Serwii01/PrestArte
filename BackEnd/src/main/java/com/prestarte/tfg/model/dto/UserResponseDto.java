package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO público de un usuario.
 *
 * Lo utilizan los endpoints de administración, el registro y la
 * consulta del perfil propio. No contiene nunca la contraseña ni los
 * bytes del documento de verificación; en su lugar expone el
 * identificador, el nombre y el tipo MIME, suficientes para que la
 * interfaz pueda enlazar la descarga a través de
 * {@code /api/files/{id}}.
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

    /** Identificador UUID del documento de verificación. */
    private String verificationFileId;

    /** Nombre original del documento (por ejemplo, "DNI_juan.pdf"). */
    private String verificationFileName;

    /** Tipo MIME del documento (por ejemplo, "application/pdf"). */
    private String verificationFileType;
}
