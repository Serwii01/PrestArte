package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Informe de instalaciones ("facility report") de una fundación.
 *
 * Es el documento estándar con el que una institución describe a los
 * prestadores sus condiciones de clima, iluminación, seguridad y acceso.
 * En PrestArte se rellena una sola vez y se reutiliza en todas las
 * solicitudes; además puede adjuntarse el PDF oficial (modelos UKRG o
 * AAM). Caduca a los 12 meses: un informe antiguo genera un aviso.
 *
 * Contiene datos de seguridad sensibles, por lo que solo lo ven la
 * propia fundación, la administración y los coleccionistas que han
 * recibido una solicitud suya.
 */
@Entity
@Table(name = "facility_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityReport {

    /** Meses tras los que el informe se considera desactualizado. */
    public static final int VALIDITY_MONTHS = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foundation_id", nullable = false, unique = true)
    private Foundation foundation;

    // ===== Clima =====

    /** Si las salas tienen climatización controlada y registrada. */
    private Boolean climateControlled;

    /** Rango de temperatura que la sala mantiene de forma estable (°C). */
    private Double tempMin;
    private Double tempMax;

    /** Rango de humedad relativa que la sala mantiene (%). */
    private Double rhMin;
    private Double rhMax;

    // ===== Iluminación =====

    /** Nivel mínimo de luz al que pueden limitar la iluminación de una obra (lux). */
    private Integer minAchievableLux;

    /** Filtrado de radiación ultravioleta. */
    private Boolean uvFiltered;

    // ===== Seguridad =====

    private Boolean guards24h;
    private Boolean cctv;
    private Boolean intrusionAlarm;
    private Boolean fireDetection;

    /** Sistema de extinción (rociadores, gas inerte...), en texto libre. */
    @Column(length = 200)
    private String fireSuppression;

    /** Disponibilidad de vitrinas para obras que las requieran. */
    private Boolean displayCasesAvailable;

    // ===== Acceso =====

    /** Medidas del acceso más pequeño hasta la sala (cm), para obras grandes. */
    private Double accessWidthCm;
    private Double accessHeightCm;

    /** Muelle de carga para camiones de transporte de arte. */
    private Boolean loadingDock;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** PDF del informe oficial (UKRG, AAM...), opcional. */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_file_id")
    private DBFile document;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Indica si el informe se actualizó hace menos de {@link #VALIDITY_MONTHS} meses. */
    public boolean isCurrent() {
        return updatedAt != null && updatedAt.isAfter(LocalDateTime.now().minusMonths(VALIDITY_MONTHS));
    }
}
