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

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final ArtworkFileRepository artworkFileRepository;
    private final ArtworkRepository artworkRepository;
    private final DBFileRepository dbFileRepository; // Inyectamos la instancia del repositorio

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
                .type(ArtworkFile.FileType.IMAGE_MAIN) // ASIGNAMOS TIPO (Obligatorio)
                .description("Foto subida desde Postman")
                .build();

        return artworkFileRepository.save(artworkFile);
    }

    public DBFile getFile(String id) {
        // Usamos la instancia 'dbFileRepository' en minúscula
        return dbFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado con ID: " + id));
    }
}