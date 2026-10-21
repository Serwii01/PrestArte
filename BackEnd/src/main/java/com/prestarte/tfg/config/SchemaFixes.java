package com.prestarte.tfg.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ajustes de esquema que {@code ddl-auto=update} no hace por sí solo.
 *
 * Hibernate crea las columnas de enums como ENUM de MySQL y después no
 * las modifica: si se añade un valor nuevo al enum, insertarlo falla.
 * Aquí se pasan a VARCHAR las columnas afectadas. Es idempotente y, si la
 * base no es MySQL (tests), no hace nada. Cuando el proyecto pase a
 * migraciones versionadas (Flyway), esto se convierte en una migración.
 */
@Component
@RequiredArgsConstructor
public class SchemaFixes implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaFixes.class);

    /** Tabla, columna y definición nueva. */
    private static final List<String[]> ENUM_TO_VARCHAR = List.<String[]>of(
            // Nuevas fases: antes de desmontar (PRE_RETURN) y traslado de gira (TRANSFER).
            new String[]{"condition_reports", "stage", "VARCHAR(20) NOT NULL"});

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] fix : ENUM_TO_VARCHAR) {
            try {
                List<String> types = jdbc.queryForList(
                        "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                        String.class, fix[0], fix[1]);
                if (!types.isEmpty() && "enum".equalsIgnoreCase(types.get(0))) {
                    jdbc.execute("ALTER TABLE " + fix[0] + " MODIFY " + fix[1] + " " + fix[2]);
                    log.info("Columna {}.{} convertida de ENUM a {}", fix[0], fix[1], fix[2]);
                }
            } catch (RuntimeException e) {
                log.debug("Ajuste de esquema omitido para {}.{}: {}", fix[0], fix[1], e.getMessage());
            }
        }
    }
}
