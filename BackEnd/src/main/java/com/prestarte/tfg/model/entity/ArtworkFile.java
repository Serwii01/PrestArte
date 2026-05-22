package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Asociación entre una {@link Artwork} y un archivo binario almacenado
 * en {@link DBFile}.
 *
 * El campo {@link #type} indica si el archivo es una fotografía de la
 * obra (con su variante: principal, detalle, lateral o trasera) o un
 * documento adjunto como certificados, seguros o informes de condición.
 * Los documentos pueden además marcarse como confidenciales para que
 * solo el dueño y la administración tengan acceso a ellos.
 */
@Entity
@Table(name = "artwork_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Obra a la que pertenece este archivo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    /**
     * Archivo binario asociado. Se persiste en cascada, de forma que al
     * eliminar el {@code ArtworkFile} también se elimina el {@code DBFile}.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "file_id", nullable = false)
    private DBFile file;

    /** Naturaleza del archivo: imagen de la obra o documento adjunto. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType type;

    /** Descripción libre asociada al archivo (por ejemplo, "Seguro 2026"). */
    @Column(length = 200)
    private String description;

    /**
     * Marca un documento como confidencial. Cuando es true, el DTO de la
     * obra omite este archivo para terceros: solo el coleccionista y los
     * administradores reciben su identificador.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean confidential = false;

    /** Categoría del archivo dentro de la ficha de la obra. */
    public enum FileType {
        /** Fotografía principal mostrada como portada en el catálogo. */
        IMAGE_MAIN,
        /** Fotografía de detalle (textura, firma, marco). */
        IMAGE_DETAIL,
        /** Fotografía lateral. */
        IMAGE_SIDE,
        /** Fotografía trasera. */
        IMAGE_BACK,
        /** Documento adjunto: certificado, seguro, factura, informe... */
        DOCUMENT
    }
}
