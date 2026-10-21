package com.prestarte.tfg.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Archivo binario almacenado en la base de datos.
 *
 * Centraliza el almacenamiento de cualquier fichero subido a la
 * plataforma: fotografías de obras, documentación adjunta, documentos de
 * verificación KYB y adjuntos del chat. Cada {@code DBFile} se identifica
 * con un UUID que se utiliza en la URL pública de descarga
 * ({@code GET /api/files/{id}}).
 */
@Entity
@Table(name = "db_files")
@EntityListeners(DBFileStorageListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DBFile {

    /** Identificador único generado automáticamente como UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Nombre original del archivo tal y como lo subió el usuario. */
    @Column(nullable = false, length = 255)
    private String fileName;

    /** Tipo MIME del contenido (por ejemplo, "image/jpeg" o "application/pdf"). */
    @Column(nullable = false, length = 50)
    private String fileType;

    /**
     * Bytes de los archivos antiguos. Los nuevos se guardan en disco (ver
     * {@link #storageKey}) y aquí queda nulo. No se serializa en JSON;
     * el contenido se entrega exclusivamente a través del endpoint
     * de descarga.
     */
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] data;

    /**
     * Clave del contenido en disco ({@link com.prestarte.tfg.service.FileBlobStore}).
     * Los archivos antiguos, anteriores a este cambio, la tienen nula y
     * guardan sus bytes en {@code data}.
     */
    @Column(length = 32)
    @JsonIgnore
    private String storageKey;

    /** Tamaño en bytes, útil para mostrarlo en la interfaz. */
    private Long fileSize;

    @CreationTimestamp
    private LocalDateTime uploadDate;
}
