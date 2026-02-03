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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private DBFile file;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType type;

    @Column(length = 100)
    private String description;  // "Foto frontal", "Detalle marco"

    public enum FileType {
        IMAGE_MAIN,     // foto principal
        IMAGE_DETAIL,   // foto detalle
        IMAGE_SIDE,     // lateral
        IMAGE_BACK,     // trasera
        DOCUMENT        // certificado, informe
    }
}
