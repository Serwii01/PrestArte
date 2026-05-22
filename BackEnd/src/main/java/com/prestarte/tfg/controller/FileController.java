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

/**
 * Endpoints REST relacionados con los archivos binarios.
 *
 * Permiten subir una imagen asociada a una obra y descargar cualquier
 * archivo a partir de su UUID. La descarga es de acceso público,
 * pensada para que el frontend pueda mostrar imágenes sin necesidad
 * de autenticación adicional.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** Sube una fotografía y la asocia a la obra indicada. */
    @PostMapping("/upload/artwork/{artworkId}")
    public String uploadArtworkFile(@PathVariable Long artworkId, @RequestParam("file") MultipartFile file) {
        try {
            ArtworkFile uploaded = fileStorageService.storeArtworkFile(artworkId, file);
            return "Archivo subido correctamente: " + uploaded.getFile().getFileName();
        } catch (IOException e) {
            return "Error al subir el archivo: " + e.getMessage();
        }
    }

    /** Sirve el contenido de un archivo a partir de su identificador UUID. */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        DBFile dbFile = fileStorageService.getFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dbFile.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + dbFile.getFileName() + "\"")
                .body(dbFile.getData());
    }
}
