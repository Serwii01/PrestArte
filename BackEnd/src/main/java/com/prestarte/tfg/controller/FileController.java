package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.entity.ArtworkFile;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload/artwork/{artworkId}")
    public String uploadArtworkFile(@PathVariable Long artworkId, @RequestParam("file") MultipartFile file) {
        try {
            ArtworkFile uploaded = fileStorageService.storeArtworkFile(artworkId, file);
            return "Archivo subido correctamente: " + uploaded.getFile().getFileName();
        } catch (IOException e) {
            return "Error al subir el archivo: " + e.getMessage();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        DBFile dbFile = fileStorageService.getFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dbFile.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + dbFile.getFileName() + "\"")
                .body(dbFile.getData());
    }
}