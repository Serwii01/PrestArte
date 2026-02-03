package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collector;

@Entity                    // ← AÑADIDO (¡importante!)
@Table(name = "artworks")  // ← AÑADIDO
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // ← Long (no long)

    @Column(length = 200, nullable = false)
    private String title;

    @Column(length = 200, nullable = false)
    private String artist;

    @Column
    private Integer year;

    @Column
    private Double widthCm;

    @Column
    private Double heightCm;

    @Column(nullable = true)
    private Double depthCm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Condition condition;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String loanConditions;

    // Relación con coleccionista
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private Collector collector;

    // Fotos/documentos
    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL)
    private List<ArtworkFile> files;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Condition {
        EXCELENTE, BUENO, REGULAR, DEFECTUOSO, DAÑADO
    }

}
