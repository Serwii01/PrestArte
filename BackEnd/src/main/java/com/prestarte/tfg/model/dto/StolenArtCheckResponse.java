package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Bases de obras robadas que consultar para una obra, y resultados automáticos si los hay. */
@Data
@Builder
public class StolenArtCheckResponse {

    /** Datos de la obra listos para pegar en los buscadores. */
    private String searchTerms;

    private List<Item> registries;

    /**
     * Una base. {@code automated} indica que se ha consultado sola; entonces
     * {@code outcome} es NO_MATCH, POSSIBLE_MATCH o ERROR.
     */
    public record Item(String code, String name, String url, String access, String scope,
                       boolean automated, String outcome, String detail, String reference) {
    }
}
