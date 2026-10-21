package com.prestarte.tfg.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** Alta o edición de una exposición. */
@Data
public class ExhibitionRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotBlank(message = "Indica la sala o sede")
    @Size(max = 200)
    private String venueName;

    @NotBlank(message = "Indica la ciudad de la sede")
    @Size(max = 150)
    private String venueCity;

    /** País de la sede (ISO de dos letras). Si no se indica, España ("ES"). */
    @Pattern(regexp = "[A-Z]{2}", message = "País no válido")
    private String venueCountry;

    @NotNull(message = "La fecha de apertura es obligatoria")
    private LocalDate openingDate;

    @NotNull(message = "La fecha de cierre es obligatoria")
    private LocalDate closingDate;

    @JsonIgnore
    @AssertTrue(message = "La fecha de cierre debe ser posterior a la de apertura")
    public boolean isDateRangeValid() {
        return openingDate == null || closingDate == null || closingDate.isAfter(openingDate);
    }

    /** Sedes siguientes si la exposición es itinerante, en orden de gira. */
    @Size(max = 10, message = "Como máximo 10 sedes adicionales")
    private List<@Valid Stop> stops;

    @Data
    public static class Stop {
        @NotBlank(message = "Indica la institución de cada sede")
        @Size(max = 200)
        private String hostInstitution;

        @NotBlank(message = "Indica la sala de cada sede")
        @Size(max = 200)
        private String venueName;

        @NotBlank(message = "Indica la ciudad de cada sede")
        @Size(max = 150)
        private String venueCity;

        @Pattern(regexp = "[A-Z]{2}", message = "País no válido")
        private String venueCountry;

        @NotNull(message = "Indica la apertura de cada sede")
        private LocalDate openingDate;

        @NotNull(message = "Indica el cierre de cada sede")
        private LocalDate closingDate;
    }
}
