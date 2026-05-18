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
 *
 * Para KYB exponemos el id, nombre y tipo MIME del documento de verificación
 * (no los bytes): así el admin puede mostrar/descargar el adjunto desde
 * /api/files/{id} sin tener que hacer un endpoint extra.
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

    /** UUID del documento de verificación (KYB). Null si no hay. */
    private String verificationFileId;
    /** Nombre original del documento (ej. "DNI_juan.pdf"). */
    private String verificationFileName;
    /** Tipo MIME del documento (ej. "application/pdf", "image/jpeg"). */
    private String verificationFileType;
}
