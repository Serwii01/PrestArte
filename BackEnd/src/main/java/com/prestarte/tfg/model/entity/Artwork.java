package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "artworks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // En Artwork.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false) // Cambiamos el nombre de la columna en BD
    private User owner;


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
    @Column(name = "artwork_condition", nullable = false)
    private Condition condition;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String loanConditions;

    // CORREGIDO: Ahora usa tu entidad Collector, no el de java.util
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = true)
    private Collector collector;

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