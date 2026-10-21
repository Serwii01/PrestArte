package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.CompatibilityReport;
import com.prestarte.tfg.model.dto.CompatibilityReport.Issue;
import com.prestarte.tfg.model.dto.CompatibilityReport.Result;
import com.prestarte.tfg.model.dto.CompatibilityReport.Severity;
import com.prestarte.tfg.model.dto.ExhibitionCompatibility;
import com.prestarte.tfg.model.entity.CourierMode;
import com.prestarte.tfg.model.entity.Exhibition;
import com.prestarte.tfg.model.entity.FacilityReport;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.LoanRequirements;
import com.prestarte.tfg.repository.FacilityReportRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Compara una exposición entera con las instalaciones de la institución.
 *
 * Además de revisar cada obra por separado, comprueba si las obras pueden
 * convivir en la misma sala: si una pide 18-20 °C y otra 22-24 °C, ninguna
 * sala sirve para las dos, aunque la institución cumpla cada requisito
 * por separado.
 */
@Service
@RequiredArgsConstructor
public class ExhibitionCompatibilityService {

    /** Solicitudes que ya no cuentan para la exposición. */
    private static final Set<LoanRequest.Status> DISCARDED =
            EnumSet.of(LoanRequest.Status.REJECTED, LoanRequest.Status.CANCELLED);

    private final ExhibitionService exhibitionService;
    private final LoanRequestRepository loanRequestRepository;
    private final FacilityReportRepository facilityReportRepository;
    private final CompatibilityService compatibilityService;

    @Transactional(readOnly = true)
    public ExhibitionCompatibility check(Long exhibitionId) {
        Exhibition ex = exhibitionService.getAsOrganizer(exhibitionId);
        FacilityReport facility = facilityReportRepository.findByFoundationId(ex.getFoundation().getId()).orElse(null);
        List<LoanRequest> loans = loanRequestRepository.findByExhibitionId(exhibitionId).stream()
                .filter(l -> !DISCARDED.contains(l.getStatus()))
                .sorted(Comparator.comparing(LoanRequest::getId))
                .toList();

        List<ExhibitionCompatibility.ArtworkResult> perArtwork = loans.stream()
                .map(l -> ExhibitionCompatibility.ArtworkResult.builder()
                        .loanId(l.getId())
                        .artworkTitle(l.getArtwork().getTitle())
                        .status(l.getStatus().name())
                        .report(compatibilityService.check(l.getArtwork(), facility, null))
                        .build())
                .toList();

        List<Issue> shared = new ArrayList<>();
        Range temp = intersect(loans, LoanRequirements::getTempMin, LoanRequirements::getTempMax);
        Range rh = intersect(loans, LoanRequirements::getRhMin, LoanRequirements::getRhMax);
        if (temp.conflict() != null) {
            shared.add(new Issue(Severity.BLOCKER, "CLIMATE", "Temperatura incompatible entre obras: " + temp.conflict()
                    + ". No pueden compartir sala; necesitan espacios o vitrinas climatizadas distintas."));
        }
        if (rh.conflict() != null) {
            shared.add(new Issue(Severity.BLOCKER, "CLIMATE", "Humedad relativa incompatible entre obras: " + rh.conflict()
                    + ". No pueden compartir sala; necesitan espacios o vitrinas climatizadas distintas."));
        }

        // La obra más sensible a la luz marca el límite si comparten sala.
        LoanRequest darkest = loans.stream()
                .filter(l -> req(l) != null && req(l).getMaxLux() != null)
                .min(Comparator.comparing(l -> req(l).getMaxLux()))
                .orElse(null);
        long luxLimited = loans.stream().filter(l -> req(l) != null && req(l).getMaxLux() != null).count();
        if (darkest != null && luxLimited < loans.size()) {
            shared.add(new Issue(Severity.WARNING, "LIGHT", "«" + darkest.getArtwork().getTitle() + "» admite como máximo "
                    + req(darkest).getMaxLux() + " lux: si comparte sala con otras obras, toda la sala debe limitarse a esa luz."));
        }

        int couriers = (int) loans.stream()
                .filter(l -> req(l) != null && req(l).getCourierMode() == CourierMode.IN_PERSON)
                .count();
        if (couriers > 1) {
            shared.add(new Issue(Severity.WARNING, "ACCESS", couriers + " obras exigen correo presencial: "
                    + "coordina los montajes para que los correos no coincidan o se solapen lo mínimo."));
        }

        if (ex.isItinerant()) {
            shared.add(new Issue(Severity.WARNING, "REPORT", "Exposición itinerante: esta comprobación usa tus "
                    + "instalaciones. Las otras " + ex.getStops().size() + " sedes deben enviar su propio informe "
                    + "de instalaciones a los prestadores."));
        }

        Result worst = worst(shared, perArtwork);
        return ExhibitionCompatibility.builder()
                .result(worst)
                .sharedIssues(shared)
                .tempMin(temp.conflict() == null ? temp.min() : null)
                .tempMax(temp.conflict() == null ? temp.max() : null)
                .rhMin(rh.conflict() == null ? rh.min() : null)
                .rhMax(rh.conflict() == null ? rh.max() : null)
                .maxLux(darkest != null ? req(darkest).getMaxLux() : null)
                .maxLuxArtwork(darkest != null ? darkest.getArtwork().getTitle() : null)
                .courierCount(couriers)
                .facilityReportUpdatedAt(facility != null ? facility.getUpdatedAt() : null)
                .artworks(perArtwork)
                .build();
    }

    // ===== Auxiliares =====

    /** Rango común; si no existe, {@code conflict} explica qué dos obras chocan. */
    private record Range(Double min, Double max, String conflict) {
    }

    private Range intersect(List<LoanRequest> loans,
                            Function<LoanRequirements, Double> minOf,
                            Function<LoanRequirements, Double> maxOf) {
        Double min = null, max = null;
        LoanRequest minOwner = null, maxOwner = null;
        for (LoanRequest l : loans) {
            LoanRequirements r = req(l);
            if (r == null) continue;
            Double lo = minOf.apply(r), hi = maxOf.apply(r);
            if (lo != null && (min == null || lo > min)) {
                min = lo;
                minOwner = l;
            }
            if (hi != null && (max == null || hi < max)) {
                max = hi;
                maxOwner = l;
            }
        }
        if (min != null && max != null && min > max) {
            return new Range(min, max, "«" + minOwner.getArtwork().getTitle() + "» necesita al menos " + fmt(min)
                    + " y «" + maxOwner.getArtwork().getTitle() + "» como mucho " + fmt(max));
        }
        return new Range(min, max, null);
    }

    private static LoanRequirements req(LoanRequest l) {
        return l.getArtwork().getRequirements();
    }

    private static Result worst(List<Issue> shared, List<ExhibitionCompatibility.ArtworkResult> perArtwork) {
        boolean blocker = shared.stream().anyMatch(i -> i.getSeverity() == Severity.BLOCKER)
                || perArtwork.stream().anyMatch(a -> a.getReport().getResult() == Result.BLOCKERS);
        if (blocker) return Result.BLOCKERS;
        boolean warning = !shared.isEmpty()
                || perArtwork.stream().anyMatch(a -> a.getReport().getResult() == Result.WARNINGS);
        return warning ? Result.WARNINGS : Result.OK;
    }

    private static String fmt(Double d) {
        return d == Math.floor(d) ? String.valueOf(d.intValue()) : String.valueOf(d);
    }
}
