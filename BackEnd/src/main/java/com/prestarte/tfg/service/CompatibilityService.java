package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.CompatibilityReport;
import com.prestarte.tfg.model.dto.CompatibilityReport.Issue;
import com.prestarte.tfg.model.dto.CompatibilityReport.Severity;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.FacilityReport;
import com.prestarte.tfg.model.entity.LoanRequirements;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Compara los requisitos de préstamo de una obra con el informe de
 * instalaciones de una fundación.
 *
 * Distingue dos niveles: {@link Severity#BLOCKER} cuando la institución
 * declara no cumplir un requisito, y {@link Severity#WARNING} cuando no
 * informa de ese dato o su informe está desactualizado. No bloquea la
 * solicitud: la decisión final es siempre del coleccionista, que ve el
 * mismo resultado antes de aceptar.
 */
@Service
public class CompatibilityService {

    /**
     * Compara la obra con el informe de la fundación. {@code startDate}
     * es opcional: si se indica, se comprueba también la antelación mínima
     * respecto a hoy.
     */
    public CompatibilityReport check(Artwork artwork, FacilityReport report, LocalDate startDate) {
        List<Issue> issues = new ArrayList<>();
        LoanRequirements req = artwork.getRequirements();

        checkNotice(req, startDate, LocalDate.now(), issues);

        if (report == null) {
            issues.add(new Issue(Severity.WARNING, "REPORT",
                    "La institución todavía no ha completado su informe de instalaciones."));
            return build(issues, null);
        }
        if (!report.isCurrent()) {
            issues.add(new Issue(Severity.WARNING, "REPORT",
                    "El informe de instalaciones tiene más de " + FacilityReport.VALIDITY_MONTHS
                            + " meses; conviene pedir que lo actualicen."));
        }

        if (req != null) {
            checkRange("CLIMATE", "temperatura", "°C", req.getTempMin(), req.getTempMax(),
                    report.getTempMin(), report.getTempMax(), issues);
            checkRange("CLIMATE", "humedad relativa", "%", req.getRhMin(), req.getRhMax(),
                    report.getRhMin(), report.getRhMax(), issues);
            checkFlag("CLIMATE", "climatización controlada", req.getClimateControl(),
                    report.getClimateControlled(), issues);

            if (req.getMaxLux() != null) {
                if (report.getMinAchievableLux() == null) {
                    issues.add(new Issue(Severity.WARNING, "LIGHT",
                            "La obra admite como máximo " + req.getMaxLux()
                                    + " lux y la institución no indica a cuánto puede limitar la luz."));
                } else if (report.getMinAchievableLux() > req.getMaxLux()) {
                    issues.add(new Issue(Severity.BLOCKER, "LIGHT",
                            "La obra admite como máximo " + req.getMaxLux() + " lux, pero la sala no baja de "
                                    + report.getMinAchievableLux() + " lux."));
                }
            }

            checkFlag("SECURITY", "vitrina", req.getDisplayCase(), report.getDisplayCasesAvailable(), issues);
            checkFlag("SECURITY", "vigilancia 24 h", req.getGuards24h(), report.getGuards24h(), issues);
            checkFlag("SECURITY", "videovigilancia (CCTV)", req.getCctv(), report.getCctv(), issues);
            checkFlag("SECURITY", "alarma de intrusión", req.getIntrusionAlarm(), report.getIntrusionAlarm(), issues);
            checkFlag("SECURITY", "detección de incendios", req.getFireDetection(), report.getFireDetection(), issues);
        }

        checkAccess(artwork, report, issues);
        return build(issues, report);
    }

    // ===== Comprobaciones =====

    private void checkNotice(LoanRequirements req, LocalDate startDate, LocalDate today, List<Issue> issues) {
        if (req == null || req.getMinNoticeDays() == null || startDate == null) return;
        long days = ChronoUnit.DAYS.between(today, startDate);
        if (days < req.getMinNoticeDays()) {
            issues.add(new Issue(Severity.BLOCKER, "NOTICE",
                    "La obra exige solicitarla con al menos " + req.getMinNoticeDays()
                            + " días de antelación y faltan " + Math.max(days, 0) + " para el inicio."));
        }
    }

    /** El rango que mantiene la sala debe quedar dentro del rango que exige la obra. */
    private void checkRange(String area, String what, String unit,
                            Double reqMin, Double reqMax, Double hasMin, Double hasMax, List<Issue> issues) {
        if (reqMin == null && reqMax == null) return;
        String required = range(reqMin, reqMax, unit);
        if ((reqMin != null && hasMin == null) || (reqMax != null && hasMax == null)) {
            issues.add(new Issue(Severity.WARNING, area,
                    "La obra requiere una " + what + " de " + required
                            + " y la institución no informa de este dato."));
            return;
        }
        boolean belowMin = reqMin != null && hasMin < reqMin;
        boolean aboveMax = reqMax != null && hasMax > reqMax;
        if (belowMin || aboveMax) {
            issues.add(new Issue(Severity.BLOCKER, area,
                    "La obra requiere una " + what + " de " + required + " y la sala mantiene "
                            + range(hasMin, hasMax, unit) + "."));
        }
    }

    private void checkFlag(String area, String what, Boolean required, Boolean available, List<Issue> issues) {
        if (!Boolean.TRUE.equals(required)) return;
        if (available == null) {
            issues.add(new Issue(Severity.WARNING, area,
                    "La obra requiere " + what + " y la institución no informa de ello."));
        } else if (!available) {
            issues.add(new Issue(Severity.BLOCKER, area,
                    "La obra requiere " + what + " y la institución declara no tenerla."));
        }
    }

    /** Comprueba que la obra (sin contar la caja) pasa por el acceso más pequeño. */
    private void checkAccess(Artwork a, FacilityReport report, List<Issue> issues) {
        Double w = a.getWidthCm(), h = a.getHeightCm();
        Double doorW = report.getAccessWidthCm(), doorH = report.getAccessHeightCm();
        if (w == null || h == null || doorW == null || doorH == null) return;
        // Holgura en cada orientación (normal y girada 90°); negativa si no cabe.
        double upright = Math.min(doorW - w, doorH - h);
        double rotated = Math.min(doorW - h, doorH - w);
        double margin = Math.max(upright, rotated);
        if (margin < 0) {
            issues.add(new Issue(Severity.BLOCKER, "ACCESS",
                    "La obra mide " + fmt(w) + " × " + fmt(h) + " cm y el acceso a la sala es de "
                            + fmt(doorW) + " × " + fmt(doorH) + " cm."));
        } else if (margin < 20) {
            issues.add(new Issue(Severity.WARNING, "ACCESS",
                    "La obra cabe muy justa por el acceso a la sala; con la caja de transporte puede no pasar."));
        }
    }

    // ===== Utilidades =====

    private CompatibilityReport build(List<Issue> issues, FacilityReport report) {
        CompatibilityReport.Result result = issues.stream().anyMatch(i -> i.getSeverity() == Severity.BLOCKER)
                ? CompatibilityReport.Result.BLOCKERS
                : issues.isEmpty() ? CompatibilityReport.Result.OK : CompatibilityReport.Result.WARNINGS;
        return CompatibilityReport.builder()
                .result(result)
                .issues(issues)
                .facilityReportUpdatedAt(report != null ? report.getUpdatedAt() : null)
                .build();
    }

    private static String range(Double min, Double max, String unit) {
        if (min != null && max != null) return fmt(min) + "–" + fmt(max) + " " + unit;
        if (min != null) return "al menos " + fmt(min) + " " + unit;
        return "como máximo " + fmt(max) + " " + unit;
    }

    private static String fmt(Double d) {
        return d == Math.rint(d) ? String.valueOf(d.longValue()) : String.valueOf(d);
    }
}
