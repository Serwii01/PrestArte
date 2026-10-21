package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.CostEstimate;
import com.prestarte.tfg.model.dto.CostEstimate.Line;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ShipmentRepository;
import com.prestarte.tfg.repository.TransportCompanyRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Estimación orientativa del coste y la huella de carbono de un préstamo.
 *
 * Responde a una queja habitual del sector: los costes (transporte,
 * cajas, seguro, correos) se conocen demasiado tarde. Aquí se calculan
 * desde antes de pedir la obra, con la tarifa que haya publicado el
 * transportista o, si no la tiene, con una tarifa de referencia.
 *
 * Todas las cifras de referencia son supuestos razonables pero NO datos
 * de mercado verificados: deben calibrarse con presupuestos reales. Por
 * eso cada estimación devuelve la lista de supuestos usados.
 */
@Service
@RequiredArgsConstructor
public class CostEstimateService {

    // ===== Tarifa de referencia (calibrar con presupuestos reales) =====
    static final double REF_BASE_FEE = 400;         // € por trayecto
    static final double REF_PER_KM = 1.8;           // € por km, vehículo climatizado de arte
    static final double REF_CRATE = 1200;           // € caja a medida para una obra de ~1 m²
    static final double INSURANCE_MONTHLY_RATE = 0.0005; // 0,05 % del valor al mes
    static final double INSURANCE_MIN_PREMIUM = 150;     // €
    static final double COURIER_TRAVEL_PER_KM = 0.2;     // € por km de viaje del correo
    static final double COURIER_NIGHT = 180;             // € por noche de hotel
    static final double COURIER_PER_DIEM = 60;           // € de dietas por día

    // ===== Factores de emisión aproximados (kg CO₂e) =====
    static final double CO2_VAN_PER_KM = 0.25;           // furgoneta diésel, por km recorrido
    static final double CO2_AIR_FREIGHT_PER_TKM = 0.8;   // carga aérea, por tonelada·km
    static final double CRATED_ARTWORK_TONNES = 0.3;     // obra embalada de tamaño medio
    static final double CO2_FLIGHT_PER_PKM = 0.15;       // pasajero en avión
    static final double CO2_TRAIN_PER_PKM = 0.035;       // pasajero en tren
    static final double COURIER_FLIES_ABOVE_KM = 700;

    private final CityLocator cityLocator;
    private final ArtworkRepository artworkRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final TransportCompanyRepository transportCompanyRepository;
    private final ExhibitionService exhibitionService;
    private final ChatSessionService chatSessionService;
    private final CurrentUser currentUser;

    /**
     * Estimación previa a la solicitud: la pide la fundación para una obra
     * y una de sus exposiciones, con la tarifa de un transportista concreto
     * o, si no se indica, con el preferido de la obra o la de referencia.
     */
    @Transactional(readOnly = true)
    public CostEstimate forRequest(Long artworkId, Long exhibitionId, Long transportCompanyId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", artworkId));
        Exhibition exhibition = exhibitionService.getForLoanRequest(exhibitionId, currentUser.currentId());
        TransportCompany company = transportCompanyId != null
                ? transportCompanyRepository.findById(transportCompanyId).orElse(null)
                : artwork.getPreferredTransportCompany();
        long months = Math.max(1, ChronoUnit.MONTHS.between(exhibition.getOpeningDate(), exhibition.getClosingDate()) + 1);
        return estimate(artwork, exhibition, company, artwork.getEstimatedValue(), months);
    }

    /** Estimación de un préstamo existente, con el transportista asignado si lo hay. */
    @Transactional(readOnly = true)
    public CostEstimate forLoan(Long loanId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> ResourceNotFoundException.of("Préstamo", loanId));
        chatSessionService.requireParticipant(loan);
        TransportCompany company = shipmentRepository
                .findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(loan.getId(), Shipment.ShipmentDirection.OUTBOUND)
                .map(Shipment::getTransportCompany)
                .orElse(loan.getArtwork().getPreferredTransportCompany());
        long months = Math.max(1, ChronoUnit.MONTHS.between(loan.getStartDate(), loan.getEndDate()) + 1);
        Double value = loan.getAgreedValue() != null ? loan.getAgreedValue() : loan.getArtwork().getEstimatedValue();
        if (loan.getExhibition() == null) {
            return unavailable("Este préstamo no tiene exposición asociada, así que no se conoce el destino.");
        }
        return estimate(loan.getArtwork(), loan.getExhibition(), company, value, months);
    }

    // ===== Cálculo =====

    CostEstimate estimate(Artwork artwork, Exhibition exhibition, TransportCompany company,
                          Double value, long months) {
        var origin = cityLocator.find(artwork.getLocation());
        if (origin.isEmpty()) {
            return unavailable("No reconocemos la ciudad de la obra («" + nullToDash(artwork.getLocation())
                    + "»). Indica solo la ciudad en la ficha de la obra.");
        }
        var destination = cityLocator.find(exhibition.getVenueCity());
        if (destination.isEmpty()) {
            return unavailable("No reconocemos la ciudad de la sede («" + exhibition.getVenueCity() + "»).");
        }

        boolean air = CityLocator.needsAirFreight(origin.get(), destination.get());
        double km = air ? CityLocator.greatCircleKm(origin.get(), destination.get())
                : CityLocator.roadKm(origin.get(), destination.get());
        CourierMode courier = artwork.getRequirements() != null && artwork.getRequirements().getCourierMode() != null
                ? artwork.getRequirements().getCourierMode() : CourierMode.NONE;

        List<Line> costs = new ArrayList<>();
        List<Line> co2 = new ArrayList<>();
        List<String> assumptions = new ArrayList<>();

        // --- Transporte (ida y vuelta) ---
        boolean companyTariff = company != null && company.getTariffPerKm() != null;
        double base = company != null && company.getTariffBaseFee() != null ? company.getTariffBaseFee() : REF_BASE_FEE;
        double perKm = companyTariff ? company.getTariffPerKm() : REF_PER_KM;
        if (air) {
            costs.add(new Line("Transporte aéreo (ida y vuelta)", null,
                    "Requiere carga aérea: pide presupuesto a un transportista con experiencia internacional."));
        } else {
            double leg = base + perKm * km;
            costs.add(new Line("Transporte (ida y vuelta)", round(2 * leg),
                    "2 trayectos × (" + eur(base) + " + " + fmt(perKm) + " €/km × " + Math.round(km) + " km)"));
        }

        // --- Caja a medida (se reutiliza a la vuelta) ---
        double crateBase = company != null && company.getTariffCrate() != null ? company.getTariffCrate() : REF_CRATE;
        double area = artwork.getWidthCm() != null && artwork.getHeightCm() != null
                ? artwork.getWidthCm() * artwork.getHeightCm() / 10_000 : 1;
        costs.add(new Line("Caja de transporte a medida", round(crateBase * Math.max(1, area)),
                "Para una obra de " + fmt(area) + " m²; se reutiliza en la devolución."));

        // --- Seguro clavo a clavo ---
        if (value != null && value > 0) {
            double premium = Math.max(INSURANCE_MIN_PREMIUM, value * INSURANCE_MONTHLY_RATE * months);
            costs.add(new Line("Seguro clavo a clavo", round(premium),
                    months + " meses sobre " + eur(value) + ". La prima real depende de la aseguradora."));
        }

        // --- Canon del prestador ---
        if (artwork.getLoanFee() != null && artwork.getLoanFee() > 0) {
            costs.add(new Line("Canon de préstamo", round(artwork.getLoanFee()), "Lo fija el coleccionista."));
        }

        // --- Correo ---
        double courierCo2 = courierKm(km) * (km > COURIER_FLIES_ABOVE_KM || air ? CO2_FLIGHT_PER_PKM : CO2_TRAIN_PER_PKM);
        if (courier == CourierMode.IN_PERSON) {
            double travel = courierKm(km) * COURIER_TRAVEL_PER_KM;
            double stay = 4 * COURIER_NIGHT + 4 * COURIER_PER_DIEM;
            costs.add(new Line("Correo presencial (2 viajes)", round(travel + stay),
                    "Viajes, 4 noches de hotel y dietas. Con correo virtual este gasto desaparece."));
        } else if (courier == CourierMode.VIRTUAL) {
            costs.add(new Line("Correo virtual", 0.0, "Supervisión remota del embalaje en PrestArte."));
        }

        // --- Huella de carbono ---
        if (air) {
            co2.add(new Line("Carga aérea (ida y vuelta)",
                    round(2 * km * CRATED_ARTWORK_TONNES * CO2_AIR_FREIGHT_PER_TKM),
                    "Obra embalada de ~" + fmt(CRATED_ARTWORK_TONNES * 1000) + " kg."));
        } else {
            co2.add(new Line("Vehículo de transporte", round(4 * km * CO2_VAN_PER_KM),
                    "2 trayectos de ida y vuelta del vehículo (" + Math.round(4 * km) + " km)."));
        }
        if (courier == CourierMode.IN_PERSON) {
            co2.add(new Line("Viajes del correo", round(courierCo2),
                    km > COURIER_FLIES_ABOVE_KM || air ? "En avión." : "En tren."));
        }

        double totalCost = costs.stream().filter(l -> l.getAmount() != null).mapToDouble(Line::getAmount).sum();
        double totalCo2 = co2.stream().mapToDouble(Line::getAmount).sum();

        assumptions.add(companyTariff
                ? "Tarifa publicada por " + company.getCompanyName() + "."
                : "Tarifa de referencia de PrestArte: " + eur(REF_BASE_FEE) + " por trayecto y "
                        + fmt(REF_PER_KM) + " €/km. El presupuesto real puede variar.");
        assumptions.add("Distancia " + (air ? "en línea recta" : "por carretera estimada (línea recta × 1,25)") + ".");
        assumptions.add("Seguro estimado al " + fmt(INSURANCE_MONTHLY_RATE * 100) + " % mensual del valor, mínimo "
                + eur(INSURANCE_MIN_PREMIUM) + ".");
        assumptions.add("Factores de emisión aproximados: furgoneta " + CO2_VAN_PER_KM + " kg/km, avión "
                + CO2_FLIGHT_PER_PKM + " kg/pasajero·km, tren " + CO2_TRAIN_PER_PKM + " kg/pasajero·km.");

        return CostEstimate.builder()
                .available(true)
                .origin(artwork.getLocation())
                .destination(exhibition.getVenueCity())
                .distanceKm(round(km))
                .transportMode(air ? "AIR" : "ROAD")
                .tariffSource(companyTariff ? company.getCompanyName() : null)
                .costLines(costs)
                .totalCost(round(totalCost))
                .co2Lines(co2)
                .totalCo2Kg(round(totalCo2))
                .co2SavedWithVirtualCourier(courier == CourierMode.IN_PERSON ? round(courierCo2) : null)
                .assumptions(assumptions)
                .build();
    }

    /** Km que viaja un correo: acompaña la ida y la vuelta, y vuelve/va por su cuenta (4 trayectos). */
    private static double courierKm(double km) {
        return 4 * km;
    }

    private static CostEstimate unavailable(String message) {
        return CostEstimate.builder().available(false).message(message).build();
    }

    private static double round(double v) {
        return Math.round(v);
    }

    private static String eur(double v) {
        return String.format(Locale.forLanguageTag("es-ES"), "%,.0f €", v);
    }

    private static String fmt(double v) {
        return String.format(Locale.forLanguageTag("es-ES"), "%.2f", v).replaceAll(",00$", "");
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
