package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hito de supervisión remota ("correo virtual").
 *
 * En lugar de que un correo del prestador viaje con la obra, quien la
 * manipula (transportista o institución) documenta cada fase con fotos y
 * el coleccionista la revisa desde PrestArte. Si la obra exige correo
 * virtual, no puede salir hasta que el coleccionista apruebe el embalaje
 * ({@link Stage#PACKING} a la ida, {@link Stage#REPACKING} a la vuelta).
 */
@Entity
@Table(name = "supervision_checkpoints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupervisionCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Stage stage;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "supervision_checkpoint_photos",
            joinColumns = @JoinColumn(name = "checkpoint_id"),
            inverseJoinColumns = @JoinColumn(name = "file_id"))
    private List<DBFile> photos = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    /** Lo que pide cambiar el coleccionista, si no lo aprueba. */
    @Column(length = 500)
    private String reviewNote;

    private LocalDateTime reviewedAt;

    /** Fases que se documentan. */
    public enum Stage {
        /** Embalaje en casa del prestador (ida). Bloquea la recogida si hay correo virtual. */
        PACKING,
        /** Carga en el vehículo. */
        LOADING,
        /** Descarga en la institución. */
        UNLOADING,
        /** Montaje en sala. */
        INSTALLATION,
        /** Desmontaje al cerrar la exposición. */
        DEINSTALLATION,
        /** Embalaje en la institución (vuelta). Bloquea la recogida si hay correo virtual. */
        REPACKING
    }

    public enum Status { PENDING, APPROVED, CHANGES_REQUESTED }
}
