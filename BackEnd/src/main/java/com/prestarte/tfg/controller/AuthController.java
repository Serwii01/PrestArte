package com.prestarte.tfg.controller;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.AuthResponse;
import com.prestarte.tfg.model.dto.ForgotPasswordRequest;
import com.prestarte.tfg.model.dto.LoginRequest;
import com.prestarte.tfg.model.dto.RegistrationRequest;
import com.prestarte.tfg.model.dto.ResetPasswordRequest;
import com.prestarte.tfg.model.dto.UserResponseDto;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.model.entity.UserStatus;
import com.prestarte.tfg.repository.UserRepository;
import com.prestarte.tfg.security.JwtService;
import com.prestarte.tfg.service.PasswordResetService;
import com.prestarte.tfg.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Endpoints públicos relacionados con la autenticación.
 *
 * Expone el registro de nuevos usuarios, el inicio de sesión y los dos
 * pasos del flujo de recuperación de contraseña. Las acciones
 * administrativas (aprobar o rechazar cuentas) viven en
 * {@link UserController} bajo {@code /api/admin}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    /**
     * Registra una nueva cuenta a partir de los datos del formulario y
     * del documento de verificación adjunto. La cuenta queda pendiente
     * de aprobación hasta que un administrador la revise.
     */
    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<UserResponseDto> register(@RequestPart("data") @Valid RegistrationRequest request,
                                                    @RequestPart("file") MultipartFile file) throws IOException {
        User saved = userService.registerUser(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    /**
     * Comprueba las credenciales del usuario y devuelve un JWT junto
     * con sus datos básicos. Rechaza la operación si la cuenta aún no
     * está aprobada.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException ex) {
            throw new DisabledException("Tu cuenta está pendiente de aprobación por el equipo de administración");
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Email o contraseña incorrectos");
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Email o contraseña incorrectos");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", request.getEmail()));

        String token = jwtService.generateToken(user);

        AuthResponse body = AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .expiresInMs(jwtService.getExpirationMs())
                .build();

        return ResponseEntity.ok(body);
    }

    /**
     * Atiende una solicitud de recuperación de contraseña. Responde
     * siempre con HTTP 200 aunque el correo no esté registrado, de
     * modo que no se pueda inferir qué cuentas existen.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.forgotPassword(req);
        return ResponseEntity.ok().build();
    }

    /** Cambia la contraseña del usuario asociado al token recibido. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req);
        return ResponseEntity.ok().build();
    }

    /** Compone el DTO público a partir de la entidad de usuario. */
    private UserResponseDto toDto(User u) {
        DBFile vf = u.getVerificationFile();
        return UserResponseDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .phone(u.getPhone())
                .role(u.getRole())
                .status(u.getStatus() == null ? UserStatus.PENDING : u.getStatus())
                .enabled(u.isEnabled())
                .taxId(u.getTaxId())
                .verificationFileId(vf != null ? vf.getId() : null)
                .verificationFileName(vf != null ? vf.getFileName() : null)
                .verificationFileType(vf != null ? vf.getFileType() : null)
                .build();
    }
}
