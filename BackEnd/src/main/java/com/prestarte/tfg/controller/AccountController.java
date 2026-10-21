package com.prestarte.tfg.controller;

import com.prestarte.tfg.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Derechos RGPD del usuario con sesión: exportar sus datos y darse de baja. */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /** Descarga todos los datos del usuario en JSON (derecho de acceso y portabilidad). */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mis-datos-prestarte.json\"")
                .body(accountService.exportMyData());
    }

    /** Baja de la cuenta (derecho de supresión). Exige la contraseña. */
    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody Map<String, String> body) {
        accountService.deleteMyAccount(body.get("password"));
        return ResponseEntity.noContent().build();
    }
}
