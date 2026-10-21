package com.prestarte.tfg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geocodificación con Nominatim (OpenStreetMap) para las ciudades que no
 * están en la tabla de {@link CityLocator}.
 *
 * Respeta la política de uso del servicio público: como mucho una
 * petición por segundo, un User-Agent que identifica la aplicación y
 * caché de los resultados (también de los no encontrados, durante un
 * día). Solo se envía el nombre de la ciudad, nunca direcciones.
 *
 * Se desactiva con {@code app.geocoding.enabled=false}; con mucho volumen
 * conviene apuntar {@code app.geocoding.url} a una instancia propia.
 */
@Component
public class RemoteGeocoder {

    private static final Logger log = LoggerFactory.getLogger(RemoteGeocoder.class);
    private static final Duration MISS_TTL = Duration.ofDays(1);
    private static final long MIN_INTERVAL_MS = 1100;

    private record Cached(Optional<CityLocator.Point> point, Instant at) {
    }

    private final boolean enabled;
    private final RestClient client;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private long lastCall;

    public RemoteGeocoder(@Value("${app.geocoding.enabled:true}") boolean enabled,
                          @Value("${app.geocoding.url:https://nominatim.openstreetmap.org}") String url,
                          @Value("${app.frontend.base-url:http://localhost:4200}") String siteUrl) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(4));
        this.client = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .defaultHeader("User-Agent", "PrestArte/1.0 (+" + siteUrl + ")")
                .defaultHeader("Accept-Language", "es")
                .build();
    }

    /** Coordenadas de una ciudad; vacío si no se encuentra o el servicio no responde. */
    public Optional<CityLocator.Point> find(String city) {
        if (!enabled || city == null || city.isBlank()) return Optional.empty();
        String key = city.trim().toLowerCase();
        Cached hit = cache.get(key);
        if (hit != null && (hit.point().isPresent() || hit.at().plus(MISS_TTL).isAfter(Instant.now()))) {
            return hit.point();
        }
        Optional<CityLocator.Point> point = query(city.trim());
        cache.put(key, new Cached(point, Instant.now()));
        return point;
    }

    private synchronized Optional<CityLocator.Point> query(String city) {
        long wait = lastCall + MIN_INTERVAL_MS - System.currentTimeMillis();
        try {
            if (wait > 0) Thread.sleep(wait);
            List<Map<String, Object>> results = client.get()
                    .uri(b -> b.path("/search")
                            .queryParam("q", city)
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .queryParam("featureType", "city")
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (results == null || results.isEmpty()) return Optional.empty();
            Map<String, Object> r = results.get(0);
            return Optional.of(new CityLocator.Point(
                    Double.parseDouble(String.valueOf(r.get("lat"))),
                    Double.parseDouble(String.valueOf(r.get("lon")))));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Geocodificación no disponible para «{}»: {}", city, e.getMessage());
            return Optional.empty();
        } finally {
            lastCall = System.currentTimeMillis();
        }
    }
}
