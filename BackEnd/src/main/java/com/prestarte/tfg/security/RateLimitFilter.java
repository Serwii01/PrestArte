package com.prestarte.tfg.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita el número de intentos por IP en los endpoints públicos de
 * autenticación para frenar ataques de fuerza bruta, registros masivos
 * y el abuso del envío de correos de recuperación.
 *
 * Usa una ventana fija en memoria: suficiente para una única instancia
 * del backend. Si en el futuro se despliegan varias, habría que mover
 * los contadores a un almacén compartido (por ejemplo Redis).
 *
 * La IP se toma de {@code request.getRemoteAddr()}. Detrás de un proxy
 * inverso hay que activar {@code server.forward-headers-strategy=native}
 * (ver application-prod.properties) para que refleje la IP real.
 *
 * No es un {@code @Component}: se registra dentro de la cadena de
 * Spring Security (después de CORS) para que el navegador pueda leer
 * la respuesta 429 también en desarrollo.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(int maxRequests, Duration window) {
    }

    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", new Rule(10, Duration.ofMinutes(5)),
            "/api/auth/register", new Rule(5, Duration.ofHours(1)),
            "/api/auth/forgot-password", new Rule(5, Duration.ofMinutes(15)),
            "/api/auth/reset-password", new Rule(10, Duration.ofMinutes(15))
    );

    private static final int CLEANUP_THRESHOLD = 10_000;

    private static final class Window {
        final long startMs;
        int count;

        Window(long startMs) {
            this.startMs = startMs;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !RULES.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        Rule rule = RULES.get(path);
        long now = System.currentTimeMillis();
        long windowMs = rule.window().toMillis();

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> now - w.startMs >= Duration.ofHours(1).toMillis());
        }

        String key = request.getRemoteAddr() + "|" + path;
        Window w = windows.compute(key, (k, current) -> {
            Window next = (current == null || now - current.startMs >= windowMs) ? new Window(now) : current;
            next.count++;
            return next;
        });

        if (w.count > rule.maxRequests()) {
            long retryAfter = Math.max(1, (w.startMs + windowMs - now) / 1000);
            reject(response, path, retryAfter);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String path, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        long minutes = Math.max(1, (retryAfterSeconds + 59) / 60);
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Demasiados intentos. Vuelve a probar en " + minutes + " min.\","
                + "\"path\":\"" + path + "\"}");
    }
}
