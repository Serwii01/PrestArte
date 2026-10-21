package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.StolenArtCheckResponse;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bases de obras robadas o expoliadas que la administración consulta en
 * la diligencia debida, con enlace, cómo se accede y los datos de la obra
 * listos para buscar. Si hay integraciones automáticas
 * ({@link StolenArtRegistryClient}), se ejecutan y su resultado se muestra
 * junto a cada base.
 */
@Service
@RequiredArgsConstructor
public class StolenArtRegistries {

    private static final Logger log = LoggerFactory.getLogger(StolenArtRegistries.class);

    /** Una base consultable. {@code url} puede ser nula si no hay consulta pública en línea. */
    public record Registry(String code, String name, String url, String access, String scope) {
    }

    public static final List<Registry> REGISTRIES = List.of(
            new Registry("INTERPOL", "INTERPOL — Stolen Works of Art (ID-Art)",
                    "https://www.interpol.int/Crimes/Cultural-heritage-crime/Stolen-Works-of-Art-Database",
                    "Gratuita, con registro previo o desde la app ID-Art.",
                    "Robos denunciados por las policías de los países miembros."),
            new Registry("ALR", "Art Loss Register",
                    "https://www.artloss.com/",
                    "De pago: emite un certificado de búsqueda con número de referencia.",
                    "Mayor base privada de obras robadas, perdidas o en disputa."),
            new Registry("LOSTART", "Lost Art-Datenbank (Alemania)",
                    "https://www.lostart.de/",
                    "Gratuita y pública.",
                    "Bienes expoliados durante el nazismo (1933–1945) y pérdidas de guerra."),
            new Registry("FBI", "FBI — National Stolen Art File",
                    "https://www.fbi.gov/investigate/violent-crime/art-theft/national-stolen-art-file",
                    "Gratuita y pública.",
                    "Obras robadas denunciadas en Estados Unidos o vinculadas a ese país."),
            new Registry("CARABINIERI", "Carabinieri TPC — Banca dati Leonardo (Italia)",
                    "https://tpcweb.carabinieri.it/",
                    "Gratuita y pública.",
                    "Bienes culturales sustraídos en Italia."),
            new Registry("ES_POLICE", "Brigada de Patrimonio Histórico (Policía Nacional) / UCO (Guardia Civil)",
                    null,
                    "Consulta por escrito: no hay buscador público.",
                    "Bienes culturales sustraídos en España."));

    private final ArtworkRepository artworkRepository;
    /** Integraciones automáticas; de serie no hay ninguna. */
    private final ObjectProvider<StolenArtRegistryClient> clients;
    private final CurrentUser currentUser;

    /** Bases a consultar para una obra, con los resultados automáticos si los hay. */
    @Transactional(readOnly = true)
    public StolenArtCheckResponse checkFor(Long artworkId) {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Solo la administración revisa la diligencia debida.");
        }
        Artwork a = artworkRepository.findById(artworkId)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", artworkId));
        Map<String, StolenArtRegistryClient.Result> auto = runClients(a);

        return StolenArtCheckResponse.builder()
                .searchTerms(searchTerms(a))
                .registries(REGISTRIES.stream()
                        .map(r -> {
                            StolenArtRegistryClient.Result res = auto.get(r.code());
                            return new StolenArtCheckResponse.Item(r.code(), r.name(), r.url(), r.access(), r.scope(),
                                    res != null, res != null ? res.outcome().name() : null,
                                    res != null ? res.detail() : null, res != null ? res.reference() : null);
                        })
                        .toList())
                .build();
    }

    /**
     * True si alguna integración automática ve una posible coincidencia:
     * entonces no se puede dar la obra por verificada sin explicar por qué.
     */
    public boolean hasPossibleMatch(Artwork a) {
        return runClients(a).values().stream()
                .anyMatch(r -> r.outcome() == StolenArtRegistryClient.Outcome.POSSIBLE_MATCH);
    }

    private Map<String, StolenArtRegistryClient.Result> runClients(Artwork a) {
        return clients.orderedStream().collect(Collectors.toMap(StolenArtRegistryClient::code, c -> {
            try {
                return c.search(a);
            } catch (RuntimeException e) {
                log.warn("Fallo consultando {}: {}", c.code(), e.getMessage());
                return new StolenArtRegistryClient.Result(StolenArtRegistryClient.Outcome.ERROR,
                        "No se pudo consultar: " + e.getMessage(), null);
            }
        }, (x, y) -> x));
    }

    /** Texto con los datos que piden los buscadores: artista, título, año. */
    private static String searchTerms(Artwork a) {
        return Stream.of(a.getArtist(), a.getTitle(), a.getYear() != null ? String.valueOf(a.getYear()) : null)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
    }
}
