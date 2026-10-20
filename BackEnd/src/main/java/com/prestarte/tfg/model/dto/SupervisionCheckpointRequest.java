package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.SupervisionCheckpoint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos de un hito de supervisión. Las fotos viajan como partes {@code photos}. */
@Data
public class SupervisionCheckpointRequest {

    @NotNull(message = "Indica la fase")
    private SupervisionCheckpoint.Stage stage;

    @Size(max = 2000)
    private String note;
}
