package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.UserResponseDto;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints administrativos sobre cuentas de usuario.
 *
 * Solo son accesibles para usuarios con rol ADMIN, según la
 * configuración de seguridad. Permiten revisar las altas pendientes,
 * consultar la lista completa de cuentas, aprobar o rechazar
 * solicitudes y eliminar cuentas cuando ya no son necesarias.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Devuelve las cuentas pendientes de aprobación. */
    @GetMapping("/pending-users")
    public ResponseEntity<List<UserResponseDto>> getPending() {
        List<UserResponseDto> body = userService.getPendingUsers().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(body);
    }

    /**
     * Devuelve todas las cuentas registradas, con la posibilidad de
     * filtrar por rol mediante el parámetro {@code role}.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> getAllUsers(@RequestParam(required = false) Role role) {
        List<UserResponseDto> body = userService.getAllUsers(role).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** Aprueba la cuenta indicada y la habilita para iniciar sesión. */
    @PostMapping("/approve/{id}")
    public ResponseEntity<String> approve(@PathVariable Long id) {
        userService.approveUser(id);
        return ResponseEntity.ok("Cuenta aprobada correctamente");
    }

    /** Marca la cuenta indicada como rechazada. */
    @PostMapping("/reject/{id}")
    public ResponseEntity<String> reject(@PathVariable Long id) {
        userService.rejectUser(id);
        return ResponseEntity.ok("Cuenta rechazada correctamente");
    }

    /** Elimina una cuenta siempre que no tenga contenido asociado. */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Cuenta eliminada correctamente");
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
                .status(u.getStatus())
                .enabled(u.isEnabled())
                .taxId(u.getTaxId())
                .verificationFileId(vf != null ? vf.getId() : null)
                .verificationFileName(vf != null ? vf.getFileName() : null)
                .verificationFileType(vf != null ? vf.getFileType() : null)
                .build();
    }
}
