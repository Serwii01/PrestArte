package com.prestarte.tfg.config;

import com.prestarte.tfg.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security.
 *
 * Define una cadena de filtros sin estado basada en JWT, abre el origen
 * CORS al frontend Angular y declara qué endpoints son públicos, cuáles
 * exigen autenticación y cuáles requieren el rol de administrador. Las
 * credenciales se cifran con BCrypt.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Construye la cadena de filtros HTTP. Desactiva CSRF (la API es
     * stateless y no usa formularios), declara las reglas de acceso y
     * registra el filtro JWT antes del filtro de autenticación estándar.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos del flujo de autenticación.
                        .requestMatchers("/api/auth/**").permitAll()
                        // Descarga de archivos por UUID: accesible para mostrar imágenes.
                        .requestMatchers(HttpMethod.GET, "/api/files/*").permitAll()
                        // Las obras del catálogo y sus fichas son públicas; sin embargo,
                        // el listado por coleccionista exige sesión para evitar fugas.
                        .requestMatchers(HttpMethod.GET, "/api/artworks/collector/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/artworks", "/api/artworks/*").permitAll()
                        // Perfil público de las empresas de transporte.
                        .requestMatchers(HttpMethod.GET, "/api/transport-companies",
                                "/api/transport-companies/*").permitAll()
                        // Operaciones administrativas reservadas al rol ADMIN.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Cualquier otro endpoint requiere autenticación válida.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura CORS para permitir las llamadas desde el origen del
     * frontend, exponer las cabeceras necesarias y aceptar todos los
     * métodos REST utilizados por la API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Algoritmo de cifrado de contraseñas utilizado en el registro y en el login. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Expone el AuthenticationManager por defecto para usarlo en el login. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
