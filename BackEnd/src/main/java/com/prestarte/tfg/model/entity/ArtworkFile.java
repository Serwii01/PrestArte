package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    // CAMBIO: Añadimos cascade para evitar el error de "unsaved transient instance"
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "file_id", nullable = false)
    private DBFile file;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType type;

    @Column(length = 200)
    private String description;

    /**
     * Solo aplicable a documentos (type=DOCUMENT). Si es true, el archivo solo es
     * visible/descargable por el coleccionista dueño y por el admin: ni siquiera
     * se devuelve su UUID al resto, así que el endpoint /api/files/{id} no puede
     * resolverlo aunque se intente adivinar.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean confidential = false;

    public enum FileType {
        IMAGE_MAIN,     // foto principal
        IMAGE_DETAIL,   // foto detalle
        IMAGE_SIDE,     // lateral
        IMAGE_BACK,     // trasera
        DOCUMENT        // certificado, seguro, informe, factura...
    }
}
