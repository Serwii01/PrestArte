package com.prestarte.tfg.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDto {

    private String id;
    private String fileName;
    private String fileType;        // "image/jpeg"
    private String downloadUrl;     // "/api/files/uuid/download"
    private Long fileSize;          // 2500000 bytes
    private String type;            // "IMAGE_MAIN"
    private String description;     // "Foto principal"
}
