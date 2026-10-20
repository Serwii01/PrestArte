package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** Datos de la resolución de exportación temporal. El PDF viaja como parte {@code file}. */
@Data
public class ExportPermitRequest {

    @NotBlank(message = "Indica el número de resolución")
    @Size(max = 100)
    private String resolutionNumber;

    @NotBlank(message = "Indica el órgano que la emite")
    @Size(max = 200)
    private String issuedBy;

    @NotNull(message = "Indica la fecha de la resolución")
    private LocalDate issueDate;

    @NotNull(message = "Indica la fecha límite de retorno")
    private LocalDate returnDeadline;
}
