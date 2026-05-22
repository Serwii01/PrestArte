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
 * Inicializa el usuario administrador necesario para arrancar la
 * plataforma.
 *
 * Las credenciales se leen de {@code application.properties}. La rutina
 * es idempotente: si ya existe una cuenta con el email indicado no se
 * crea ninguna otra ni se modifica la contraseña. Las contraseñas se
 * mantienen únicamente desde la interfaz para evitar sobreescrituras
 * accidentales al reiniciar la aplicación.
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

    /**
     * Comprueba la existencia del administrador inicial y lo crea si la
     * base de datos aún no contiene una cuenta con ese email.
     */
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
