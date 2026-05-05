package com.prestarte.tfg.controller;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.AuthResponse;
import com.prestarte.tfg.model.dto.LoginRequest;
import com.prestarte.tfg.model.dto.RegistrationRequest;
import com.prestarte.tfg.model.dto.UserResponseDto;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.model.entity.UserStatus;
import com.prestarte.tfg.repository.UserRepository;
import com.prestarte.tfg.security.JwtService;
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
 * Endpoints públicos de autenticación: registro y login.
 * El resto del flujo (aprobación de admin) está en UserController bajo /api/admin/**.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Registro de un nuevo usuario (Coleccionista, Fundación o Empresa de Transporte).
     * El usuario queda con status=PENDING y enabled=false hasta que un admin lo apruebe.
     */
    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<UserResponseDto> register(@RequestPart("data") @Valid RegistrationRequest request,
                                                    @RequestPart("file") MultipartFile file) throws IOException {
        User saved = userService.registerUser(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    /**
     * Login: devuelve JWT + datos básicos del usuario.
     * Rechaza si el usuario no está aprobado (enabled=false → DisabledException).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException ex) {
            // Usuario existe y password OK pero no aprobado
            throw new DisabledException("Tu cuenta está pendiente de aprobación por un administrador");
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

    private UserResponseDto toDto(User u) {
        return UserResponseDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .phone(u.getPhone())
                .role(u.getRole())
                .status(u.getStatus() == null ? UserStatus.PENDING : u.getStatus())
                .enabled(u.isEnabled())
                .taxId(u.getTaxId())
                .build();
    }
}
