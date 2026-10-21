package com.prestarte.tfg.model.dto;

import java.util.List;

/** Página de resultados de la búsqueda del catálogo. */
public record ArtworkPage(List<ArtworkDto> items, long total, int page, int size, int totalPages) {
}
