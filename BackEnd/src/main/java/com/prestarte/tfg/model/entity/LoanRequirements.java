package com.prestarte.tfg.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

/**
 * Requisitos estructurados que el coleccionista exige a la institución
 * para prestar una obra.
 *
 * Son los mismos apartados que recoge un informe de instalaciones
 * ({@link FacilityReport}), de modo que PrestArte puede comparar ambos y
 * avisar de incompatibilidades antes de que se envíe la solicitud. Todos
 * los campos son opcionales: un valor nulo significa "sin requisito".
 * El texto libre {@code Artwork.loanConditions} sigue existiendo para lo
 * que no encaja aquí.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequirements {

    /** Temperatura mínima y máxima admisibles en sala (°C). */
    @Column(name = "req_temp_min")
    private Double tempMin;
    @Column(name = "req_temp_max")
    private Double tempMax;

    /** Humedad relativa mínima y máxima admisibles (%). */
    @Column(name = "req_rh_min")
    private Double rhMin;
    @Column(name = "req_rh_max")
    private Double rhMax;

    /** Iluminación máxima sobre la obra (lux). */
    @Column(name = "req_max_lux")
    private Integer maxLux;

    @Column(name = "req_climate_control")
    private Boolean climateControl;

    @Column(name = "req_display_case")
    private Boolean displayCase;

    @Column(name = "req_guards_24h")
    private Boolean guards24h;

    @Column(name = "req_cctv")
    private Boolean cctv;

    @Column(name = "req_intrusion_alarm")
    private Boolean intrusionAlarm;

    @Column(name = "req_fire_detection")
    private Boolean fireDetection;

    /**
     * Antelación mínima con la que debe llegar una solicitud (días entre
     * la solicitud y el inicio del préstamo). Los museos suelen pedir
     * entre 6 y 12 meses.
     */
    @Column(name = "req_min_notice_days")
    private Integer minNoticeDays;

    /** Supervisión que exige el prestador. Nulo equivale a NONE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "req_courier_mode", length = 20)
    private CourierMode courierMode;
}
