package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

/**
 * Recordatorios automáticos de plazos. Se ejecuta cada mañana y avisa
 * (en la aplicación y por email) de lo que vence pronto o ya ha vencido.
 * Cada recordatorio se envía una sola vez gracias a {@link ReminderLog}.
 */
@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    static final int LOAN_ENDING_DAYS = 14;
    static final int PERMIT_DEADLINE_DAYS = 30;
    static final int INSURANCE_ENDING_DAYS = 14;
    static final int FACILITY_WARNING_DAYS = 30;
    static final int PENDING_REQUEST_DAYS = 7;
    static final int PENDING_INSURANCE_DAYS = 3;

    /** Préstamos en los que la obra ya ha salido o está a punto de salir. */
    private static final Set<LoanRequest.Status> OBJECT_MOVING = EnumSet.of(
            LoanRequest.Status.PAID, LoanRequest.Status.READY_FOR_PICKUP, LoanRequest.Status.IN_TRANSIT,
            LoanRequest.Status.DELIVERED, LoanRequest.Status.ON_LOAN, LoanRequest.Status.RETURNING);

    private final LoanRequestRepository loanRequestRepository;
    private final InsuranceCertificateRepository insuranceRepository;
    private final ExportPermitRepository permitRepository;
    private final FacilityReportRepository facilityReportRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final NotificationService notificationService;

    /** Todos los días a las 8:00 (hora de Madrid). */
    @Scheduled(cron = "${app.reminders.cron:0 0 8 * * *}", zone = "Europe/Madrid")
    @Transactional
    public void runDaily() {
        LocalDate today = LocalDate.now();
        int sent = 0;
        for (LoanRequest loan : loanRequestRepository.findAll()) {
            sent += loanReminders(loan, today);
        }
        sent += facilityReminders(today);
        if (sent > 0) log.info("Recordatorios enviados: {}", sent);
    }

    private int loanReminders(LoanRequest loan, LocalDate today) {
        int sent = 0;
        LoanRequest.Status s = loan.getStatus();
        User collector = loan.getArtwork().getCollector();
        User foundation = loan.getFoundation();
        String link = "/app/loans/" + loan.getId();
        String title = "Préstamo de «" + loan.getArtwork().getTitle() + "»";

        // Solicitud sin respuesta.
        if (s == LoanRequest.Status.REQUESTED) {
            if (daysSinceRequest(loan, today) >= PENDING_REQUEST_DAYS
                    && once("request-pending:" + loan.getId())) {
                notificationService.notify(collector, title,
                        "Tienes una solicitud de préstamo sin responder desde hace más de "
                                + PENDING_REQUEST_DAYS + " días. Acéptala o recházala para que la institución pueda planificar.",
                        link, true);
                sent++;
            }
        }

        // Fin del préstamo cercano o vencido.
        if (s == LoanRequest.Status.ON_LOAN && loan.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(today, loan.getEndDate());
            if (days < 0 && once("loan-overdue:" + loan.getId() + ":" + loan.getEndDate())) {
                String msg = "La fecha de fin del préstamo (" + loan.getEndDate()
                        + ") ha pasado y la devolución todavía no se ha iniciado.";
                notificationService.notify(foundation, title, msg + " Inicia la devolución cuanto antes.", link, true);
                notificationService.notify(collector, title, msg + " Contacta con la institución.", link, true);
                sent += 2;
            } else if (days >= 0 && days <= LOAN_ENDING_DAYS && once("loan-ending:" + loan.getId() + ":" + loan.getEndDate())) {
                String msg = "El préstamo termina el " + loan.getEndDate() + " (dentro de " + days
                        + " días). Es momento de organizar el desmontaje y la devolución.";
                notificationService.notify(foundation, title, msg, link, true);
                notificationService.notify(collector, title, msg, link, true);
                sent += 2;
            }
        }

        if (OBJECT_MOVING.contains(s)) {
            // Fecha límite de retorno del permiso de exportación.
            ExportPermit permit = permitRepository
                    .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId()).orElse(null);
            if (permit != null) {
                long days = ChronoUnit.DAYS.between(today, permit.getReturnDeadline());
                if (days < 0 && once("permit-overdue:" + permit.getId())) {
                    String msg = "Ha vencido la fecha límite de retorno del permiso de exportación ("
                            + permit.getReturnDeadline() + "). Si la obra no ha vuelto a España, la salida puede "
                            + "considerarse exportación ilícita: contactad urgentemente con el Ministerio de Cultura.";
                    notificationService.notify(foundation, title, msg, link, true);
                    notificationService.notify(collector, title, msg, link, true);
                    sent += 2;
                } else if (days >= 0 && days <= PERMIT_DEADLINE_DAYS && once("permit-deadline:" + permit.getId())) {
                    String msg = "La obra debe estar de vuelta en España antes del " + permit.getReturnDeadline()
                            + " (dentro de " + days + " días), según el permiso de exportación.";
                    notificationService.notify(foundation, title, msg, link, true);
                    notificationService.notify(collector, title, msg, link, true);
                    sent += 2;
                }
            }

            // Seguro a punto de vencer con la obra fuera.
            InsuranceCertificate cert = insuranceRepository
                    .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId()).orElse(null);
            if (cert != null && cert.getStatus() == InsuranceCertificate.ReviewStatus.APPROVED) {
                long days = ChronoUnit.DAYS.between(today, cert.getCoverageEnd());
                if (days >= 0 && days <= INSURANCE_ENDING_DAYS && once("insurance-ending:" + cert.getId())) {
                    String msg = "La cobertura del seguro termina el " + cert.getCoverageEnd()
                            + " y la obra todavía no ha vuelto. Amplía la póliza para que siga cubierta.";
                    notificationService.notify(foundation, title, msg, link, true);
                    notificationService.notify(collector, title, msg, link, true);
                    sent += 2;
                }
            }
        }

        // Certificado de seguro esperando revisión del coleccionista.
        InsuranceCertificate pending = insuranceRepository
                .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId()).orElse(null);
        if (pending != null && pending.getStatus() == InsuranceCertificate.ReviewStatus.PENDING
                && pending.getCreatedAt() != null
                && pending.getCreatedAt().isBefore(LocalDateTime.now().minusDays(PENDING_INSURANCE_DAYS))
                && once("insurance-review:" + pending.getId())) {
            notificationService.notify(collector, title,
                    "El certificado de seguro lleva más de " + PENDING_INSURANCE_DAYS
                            + " días esperando tu revisión. Sin tu aprobación la obra no puede salir.",
                    link, true);
            sent++;
        }
        return sent;
    }

    private int facilityReminders(LocalDate today) {
        int sent = 0;
        for (FacilityReport r : facilityReportRepository.findAll()) {
            if (r.getUpdatedAt() == null) continue;
            LocalDate expires = r.getUpdatedAt().toLocalDate().plusMonths(FacilityReport.VALIDITY_MONTHS);
            long days = ChronoUnit.DAYS.between(today, expires);
            // La clave incluye la fecha de actualización: si lo renuevan, el aviso vuelve a poder enviarse.
            if (days <= FACILITY_WARNING_DAYS
                    && once("facility-expiring:" + r.getId() + ":" + r.getUpdatedAt().toLocalDate())) {
                notificationService.notify(r.getFoundation(), "Informe de instalaciones",
                        days >= 0
                                ? "Tu informe de instalaciones caduca el " + expires + ". Revísalo para que tus solicitudes no lleguen con avisos."
                                : "Tu informe de instalaciones tiene más de 12 meses. Revísalo y guárdalo de nuevo.",
                        "/app/foundation/facility", true);
                sent++;
            }
        }
        return sent;
    }

    /** Devuelve true (y lo anota) si el recordatorio no se había enviado ya. */
    private boolean once(String key) {
        if (reminderLogRepository.existsByKey(key)) return false;
        reminderLogRepository.save(ReminderLog.builder().key(key).build());
        return true;
    }

    /** Días desde que se envió la solicitud (0 en solicitudes antiguas sin fecha). */
    private long daysSinceRequest(LoanRequest loan, LocalDate today) {
        return loan.getCreatedAt() != null ? ChronoUnit.DAYS.between(loan.getCreatedAt().toLocalDate(), today) : 0;
    }
}
