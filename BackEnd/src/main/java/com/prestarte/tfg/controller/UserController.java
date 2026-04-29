package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.RegistrationRequest;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Endpoint para que el usuario "aplique" al registro
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestPart("data") RegistrationRequest request,
                                         @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(userService.registerUser(request, file));
    }

    // Endpoint para el administrador (lista de pendientes)
    @GetMapping("/admin/pending-users")
    public ResponseEntity<List<User>> getPending() {
        return ResponseEntity.ok(userService.getPendingUsers());
    }

    // Endpoint para aprobar (lo que harás tú desde la URL /admin en el futuro)
    @PostMapping("/admin/approve/{id}")
    public ResponseEntity<String> approve(@PathVariable Long id) {
        userService.approveUser(id);
        return ResponseEntity.ok("Usuario aprobado correctamente");
    }
}