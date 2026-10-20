package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.InsuranceCertificate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** Datos del certificado de seguro. El PDF viaja como parte {@code file}. */
@Data
public class InsuranceCertificateRequest {

    @NotNull(message = "Indica el tipo de cobertura")
    private InsuranceCertificate.CoverageType coverageType;

    @NotBlank(message = "Indica la aseguradora")
    @Size(max = 200)
    private String insurer;

    @NotBlank(message = "Indica el número de póliza o la referencia de la orden")
    @Size(max = 100)
    private String policyNumber;

    @NotNull(message = "Indica la suma asegurada")
    @Positive(message = "La suma asegurada debe ser mayor que 0")
    private Double insuredAmount;

    @NotNull(message = "Indica desde cuándo cubre")
    private LocalDate coverageStart;

    @NotNull(message = "Indica hasta cuándo cubre")
    private LocalDate coverageEnd;
}
