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
 * Servicio que implementa el flujo de recuperación de contraseña.
 *
 * Se compone de dos pasos: la solicitud de recuperación genera un
 * token de un solo uso y lo envía por correo, y el paso de
 * restablecimiento valida el token y actualiza la contraseña. La
 * solicitud responde siempre con éxito al cliente, independientemente
 * de si el correo está registrado, para no revelar qué cuentas
 * existen.
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

    /**
     * Atiende una solicitud de recuperación de contraseña. Si el correo
     * pertenece a un usuario, se invalidan los tokens anteriores, se
     * genera uno nuevo y se envía por correo electrónico. Si no
     * pertenece a nadie, la operación termina sin hacer nada.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        Optional<User> maybeUser = userRepository.findByEmail(req.getEmail());
        if (maybeUser.isEmpty()) {
            log.info("Solicitud de reset para email no registrado: {}", req.getEmail());
            return;
        }

        User user = maybeUser.get();

        tokenRepository.invalidateAllForUser(user);

        PasswordResetToken token = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS))
                .used(false)
                .build();
        tokenRepository.save(token);

        sendResetEmail(user, token.getToken());
    }

    /**
     * Restablece la contraseña del usuario asociado al token indicado.
     * Comprueba que el token exista, esté vigente y no haya sido
     * utilizado, cifra la nueva contraseña con BCrypt y marca el
     * token como consumido.
     */
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

    /**
     * Envía al usuario el correo de recuperación con el enlace que
     * incluye el token. El enlace queda también registrado en el log
     * para facilitar el desarrollo en entornos sin servidor SMTP.
     */
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
        log.info("Reset link para {}: {}", user.getEmail(), link);
    }
}
