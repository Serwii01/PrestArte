package com.prestarte.tfg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas (recordatorios diarios de plazos). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
