package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.UserResponseDto;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints administrativos sobre usuarios (solo accesibles por ADMIN).
 * El registro y login se han movido a AuthController bajo /api/auth/**.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/pending-users")
    public ResponseEntity<List<UserResponseDto>> getPending() {
        List<UserResponseDto> body = userService.getPendingUsers().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<String> approve(@PathVariable Long id) {
        userService.approveUser(id);
        return ResponseEntity.ok("Cuenta aprobada correctamente");
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<String> reject(@PathVariable Long id) {
        userService.rejectUser(id);
        return ResponseEntity.ok("Cuenta rechazada correctamente");
    }

    private UserResponseDto toDto(User u) {
        return UserResponseDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .phone(u.getPhone())
                .role(u.getRole())
                .status(u.getStatus())
                .enabled(u.isEnabled())
                .taxId(u.getTaxId())
                .build();
    }
}
