package com.prestarte.tfg.model.dto;

import lombok.*;

/**
 * DTO público que representa un archivo asociado a una obra.
 *
 * Sirve tanto para las fotografías como para los documentos
 * adjuntos. El identificador UUID se utiliza para construir la URL
 * de descarga, y el identificador del {@code ArtworkFile} permite
 * borrar el adjunto desde la interfaz.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDto {

    /** Identificador UUID del archivo binario. */
    private String id;

    /** Identificador interno del {@code ArtworkFile} asociado. */
    private Long artworkFileId;

    private String fileName;

    /** Tipo MIME del archivo (por ejemplo, "image/jpeg" o "application/pdf"). */
    private String fileType;

    /** URL de descarga; el frontend la compone a partir del identificador. */
    private String downloadUrl;

    /** Tamaño en bytes, útil para mostrarlo en la interfaz. */
    private Long fileSize;

    /** Categoría del archivo dentro de la ficha: IMAGE_MAIN, DOCUMENT... */
    private String type;

    /** Descripción libre del archivo (por ejemplo, "Seguro 2026"). */
    private String description;

    /**
     * Marca un documento como confidencial. Cuando es true, el DTO
     * solo se entrega al coleccionista dueño y a la administración.
     */
    private boolean confidential;
}
