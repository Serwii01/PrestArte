package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.entity.ArtworkFile;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Endpoints REST relacionados con los archivos binarios.
 *
 * Permiten subir una imagen asociada a una obra y descargar un archivo
 * a partir de su UUID. La descarga no exige sesión para las fotos de
 * obras y los documentos públicos; el resto de archivos (documentos
 * confidenciales, verificación de cuentas, adjuntos del chat) se
 * comprueban en {@link FileStorageService#getFile(String)}.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** Sube una fotografía y la asocia a la obra indicada. */
    @PostMapping("/upload/artwork/{artworkId}")
    public String uploadArtworkFile(@PathVariable Long artworkId,
                                    @RequestParam("file") MultipartFile file) throws IOException {
        ArtworkFile uploaded = fileStorageService.storeArtworkFile(artworkId, file);
        return "Archivo subido correctamente: " + uploaded.getFile().getFileName();
    }

    /**
     * Sirve el contenido de un archivo a partir de su identificador UUID.
     * Las imágenes se muestran en línea; el resto se fuerza a descarga.
     * La cabecera CSP {@code sandbox} impide que un archivo antiguo
     * subido antes de validar tipos ejecute scripts si se abre
     * directamente en el navegador.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        DBFile dbFile = fileStorageService.getFile(id);

        MediaType type;
        try {
            type = MediaType.parseMediaType(dbFile.getFileType());
        } catch (Exception e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        boolean inline = "image".equals(type.getType())
                || MediaType.APPLICATION_PDF.equalsTypeAndSubtype(type);

        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(dbFile.getFileName(), StandardCharsets.UTF_8)
                .build();

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        // El visor de PDF de Chrome no carga con "sandbox"; los PDF no
        // ejecutan scripts de la página, así que solo se omite para ellos.
        if (!MediaType.APPLICATION_PDF.equalsTypeAndSubtype(type)) {
            response.header("Content-Security-Policy", "sandbox");
        }
        return response.body(dbFile.getData());
    }
}
