package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro de recordatorios ya enviados, para que la tarea diaria no
 * repita el mismo aviso cada mañana. La clave identifica el recordatorio
 * (tipo + entidad + umbral), p. ej. {@code loan-ending-14:12}.
 */
@Entity
@Table(name = "reminder_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reminder_key", nullable = false, unique = true, length = 120)
    private String key;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime sentAt;
}
