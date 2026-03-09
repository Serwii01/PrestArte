package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "db_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DBFile {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 50)
    private String fileType;  // "image/jpeg", "image/png", "application/pdf"

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;  // ← BYTES de la foto

    private Long fileSize;  // bytes

    @CreationTimestamp
    private LocalDateTime uploadDate;
}
