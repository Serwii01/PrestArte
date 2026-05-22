package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.ArtworkFile;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.repository.ArtworkFileRepository;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.DBFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Servicio que centraliza el almacenamiento de archivos binarios.
 *
 * Ofrece operaciones para guardar una fotografía asociada a una obra
 * y para recuperar el contenido de cualquier archivo a partir de su
 * identificador. Los bytes se almacenan en la tabla {@code db_files}
 * a través de {@link DBFile}.
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final ArtworkFileRepository artworkFileRepository;
    private final ArtworkRepository artworkRepository;
    private final DBFileRepository dbFileRepository;

    /**
     * Guarda una imagen asociada a una obra. Crea el {@link DBFile}
     * con los bytes y lo enlaza a la obra mediante un nuevo
     * {@link ArtworkFile} de tipo IMAGE_MAIN.
     */
    @Transactional
    public ArtworkFile storeArtworkFile(Long artworkId, MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        DBFile dbFile = DBFile.builder()
                .fileName(fileName)
                .fileType(file.getContentType())
                .data(file.getBytes())
                .build();

        ArtworkFile artworkFile = ArtworkFile.builder()
                .artwork(artwork)
                .file(dbFile)
                .type(ArtworkFile.FileType.IMAGE_MAIN)
                .description(fileName)
                .build();

        return artworkFileRepository.save(artworkFile);
    }

    /** Recupera un archivo a partir de su UUID. */
    public DBFile getFile(String id) {
        return dbFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado con ID: " + id));
    }
}
