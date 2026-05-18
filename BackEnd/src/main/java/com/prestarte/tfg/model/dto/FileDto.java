package com.prestarte.tfg.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDto {

    /** ID público del archivo (UUID del DBFile). Sirve para GET /api/files/{id}. */
    private String id;

    /** ID interno del ArtworkFile, necesario para borrar el adjunto. */
    private Long artworkFileId;

    private String fileName;
    private String fileType;        // "image/jpeg" / "application/pdf"
    private String downloadUrl;     // "/api/files/uuid"
    private Long fileSize;          // 2500000 bytes
    private String type;            // "IMAGE_MAIN" / "DOCUMENT" / ...
    private String description;     // "Seguro vigente 2026"

    /**
     * Solo para documentos (type=DOCUMENT). Si es true, solo el dueño/admin
     * recibe este DTO; el resto de usuarios no lo verán nunca.
     */
    private boolean confidential;
}
