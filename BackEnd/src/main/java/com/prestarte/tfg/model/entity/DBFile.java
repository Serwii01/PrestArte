package com.prestarte.tfg.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @GeneratedValue(strategy = GenerationType.UUID) // Esto genera el ID automáticamente
    private String id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 50)
    private String fileType;  // "image/jpeg", "image/png", "application/pdf"

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] data;  // ← BYTES de la foto

    private Long fileSize;  // bytes

    @CreationTimestamp
    private LocalDateTime uploadDate;
}
