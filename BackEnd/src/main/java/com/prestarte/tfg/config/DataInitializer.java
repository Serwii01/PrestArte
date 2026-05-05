package com.prestarte.tfg.config;

import com.prestarte.tfg.model.entity.Admin;
import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.UserStatus;
import com.prestarte.tfg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un administrador inicial al arrancar la aplicación si todavía no existe.
 * Las credenciales se leen de application.properties (app.admin.email / app.admin.password).
 *
 * Es idempotente: si el admin ya existe en BD, no hace nada.
 * Si cambias la pwd en properties y arrancas, NO actualiza la existente
 * (por seguridad: cambios de pwd se hacen desde la UI, no por reinicio).
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name:Administrador}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin '{}' ya existe. No se crea uno nuevo.", adminEmail);
            return;
        }

        Admin admin = Admin.builder()
                .email(adminEmail)
                .name(adminName)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .status(UserStatus.APPROVED)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("Administrador inicial '{}' creado.", adminEmail);
    }
}
