package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ForgotPasswordRequest;
import com.prestarte.tfg.model.dto.ResetPasswordRequest;
import com.prestarte.tfg.model.entity.PasswordResetToken;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.PasswordResetTokenRepository;
import com.prestarte.tfg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Lógica de "olvidé mi contraseña":
 *  1. forgotPassword: genera un token de un solo uso, invalida los previos del
 *     mismo usuario, y manda un email con la URL de reseteo. Devuelve siempre
 *     200 al cliente: no revelamos si el email existe o no (anti-enumeration).
 *  2. resetPassword: valida el token (existe, no expirado, no usado), actualiza
 *     la contraseña hasheada y marca el token como usado.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long TOKEN_VALIDITY_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        Optional<User> maybeUser = userRepository.findByEmail(req.getEmail());
        if (maybeUser.isEmpty()) {
            // No revelamos si el email existe.
            log.info("Solicitud de reset para email no registrado: {}", req.getEmail());
            return;
        }

        User user = maybeUser.get();

        // Invalidamos tokens previos.
        tokenRepository.invalidateAllForUser(user);

        // Generamos token nuevo.
        PasswordResetToken token = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS))
                .used(false)
                .build();
        tokenRepository.save(token);

        sendResetEmail(user, token.getToken());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken token = tokenRepository.findByToken(req.getToken())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El enlace no es válido o ha caducado. Solicita uno nuevo."));

        if (!token.isValid()) {
            throw new IllegalArgumentException(
                    "El enlace no es válido o ha caducado. Solicita uno nuevo.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Contraseña restablecida para el usuario {}", user.getEmail());
    }

    private void sendResetEmail(User user, String tokenValue) {
        String link = frontendBaseUrl + "/reset-password?token=" + tokenValue;
        String subject = "Prestarte - Restablece tu contraseña";
        String body = "Hola " + user.getName() + ",\n\n" +
                "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.\n" +
                "Pulsa el siguiente enlace para crear una contraseña nueva " +
                "(válido durante " + TOKEN_VALIDITY_HOURS + " hora):\n\n" +
                link + "\n\n" +
                "Si no has sido tú, puedes ignorar este mensaje.\n\n" +
                "Un saludo,\nEl equipo de Prestarte.";
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
        // Como fallback en local sin SMTP funcional, también lo logueamos.
        log.info("Reset link para {}: {}", user.getEmail(), link);
    }
}
