package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** La fundación pide ampliar el préstamo hasta una nueva fecha. */
@Data
public class ExtensionRequest {

    @NotNull(message = "Indica la nueva fecha de fin")
    private LocalDate newEndDate;

    @Size(max = 500)
    private String reason;
}
