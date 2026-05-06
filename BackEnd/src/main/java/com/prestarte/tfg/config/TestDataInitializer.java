package com.prestarte.tfg.config;

import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Carga un conjunto mínimo de datos de demostración: 1 coleccionista, 1 fundación,
 * 1 empresa de transporte, 1 obra y 1 solicitud de préstamo.
 *
 * Solo se activa si {@code app.seed-test-data=true}. Idempotente: si los usuarios
 * ya existen, no se duplica nada.
 *
 * Credenciales generadas (cambiar en producción):
 *   collector@test.com / Collector1234!
 *   foundation@test.com / Foundation1234!
 *   transport@test.com / Transport1234!
 */
@Component
@ConditionalOnProperty(name = "app.seed-test-data", havingValue = "true")
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    private final UserRepository userRepository;
    private final CollectorRepository collectorRepository;
    private final FoundationRepository foundationRepository;
    private final TransportCompanyRepository transportCompanyRepository;
    private final ArtworkRepository artworkRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("collector@test.com")) {
            log.info("Datos de prueba ya existen, no se vuelven a sembrar.");
            return;
        }

        Collector collector = createCollector();
        Foundation foundation = createFoundation();
        TransportCompany transport = createTransportCompany();
        Artwork artwork = createArtwork(collector);
        LoanRequest loan = createLoanRequest(artwork, foundation);

        log.info("Datos de prueba creados:");
        log.info("  Collector id={} email={}", collector.getId(), collector.getEmail());
        log.info("  Foundation id={} email={}", foundation.getId(), foundation.getEmail());
        log.info("  TransportCompany id={} email={}", transport.getId(), transport.getEmail());
        log.info("  Artwork id={} title='{}'", artwork.getId(), artwork.getTitle());
        log.info("  LoanRequest id={} status={}", loan.getId(), loan.getStatus());
    }

    private Collector createCollector() {
        Collector c = Collector.builder()
                .email("collector@test.com")
                .name("Carlos Coleccionista")
                .password(passwordEncoder.encode("Collector1234!"))
                .phone("+34 600 000 001")
                .taxId("12345678A")
                .role(Role.COLLECTOR)
                .status(UserStatus.APPROVED)
                .enabled(true)
                .address("Calle del Arte 1")
                .city("Madrid")
                .postalCode("28001")
                .build();
        return collectorRepository.save(c);
    }

    private Foundation createFoundation() {
        Foundation f = Foundation.builder()
                .email("foundation@test.com")
                .name("Fundación de Prueba")
                .password(passwordEncoder.encode("Foundation1234!"))
                .phone("+34 600 000 002")
                .taxId("B12345678")
                .role(Role.FOUNDATION)
                .status(UserStatus.APPROVED)
                .enabled(true)
                .institutionName("Museo Demo")
                .address("Plaza del Museo 5")
                .city("Barcelona")
                .build();
        return foundationRepository.save(f);
    }

    private TransportCompany createTransportCompany() {
        TransportCompany t = TransportCompany.builder()
                .email("transport@test.com")
                .name("Empresa de Transporte de Prueba")
                .password(passwordEncoder.encode("Transport1234!"))
                .phone("+34 600 000 003")
                .taxId("C12345678")
                .role(Role.TRANSPORT)
                .status(UserStatus.APPROVED)
                .enabled(true)
                .companyName("ArteSeguro Logística")
                .coverageArea("Nacional")
                .contactEmail("ops@arteseguro.test")
                .build();
        return transportCompanyRepository.save(t);
    }

    private Artwork createArtwork(Collector owner) {
        Artwork a = Artwork.builder()
                .title("Bodegón con manzanas")
                .artist("Pedro Pintor")
                .year(1923)
                .widthCm(80.0)
                .heightCm(60.0)
                .depthCm(3.0)
                .condition(Artwork.Condition.GOOD)
                .description("Óleo sobre lienzo. Restaurado en 2010.")
                .estimatedValue(45000.0)
                .loanConditions("Climatización 20±2 ºC, humedad 50±5%, sin luz directa.")
                .collector(owner)
                .build();
        return artworkRepository.save(a);
    }

    private LoanRequest createLoanRequest(Artwork artwork, Foundation foundation) {
        LoanRequest l = LoanRequest.builder()
                .artwork(artwork)
                .foundation(foundation)
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(120))
                .agreedConditions("Exposición temporal en sala principal.")
                .status(LoanRequest.Status.REQUESTED)
                .transportCompanyMandatory(false)
                .build();
        return loanRequestRepository.save(l);
    }
}
