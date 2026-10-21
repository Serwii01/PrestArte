package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.NotificationResponse;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Notification;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.NotificationRepository;
import com.prestarte.tfg.repository.ShipmentRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Avisos a los usuarios: se guardan para la campana de la aplicación y,
 * cuando son importantes, también se envían por email.
 *
 * Lo usan el historial del préstamo (cada hito avisa a las demás partes)
 * y la tarea diaria de recordatorios de plazos.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ShipmentRepository shipmentRepository;
    private final EmailService emailService;
    private final CurrentUser currentUser;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    // ===== Envío =====

    /** Crea un aviso para un usuario y, si se indica, le envía también un email. */
    @Transactional
    public void notify(User recipient, String title, String message, String link, boolean email) {
        notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .title(truncate(title, 200))
                .message(truncate(message, 1000))
                .link(link)
                .build());
        if (email && recipient.getEmail() != null) {
            String url = link != null ? "\n\nVer en PrestArte: " + frontendBaseUrl + link : "";
            emailService.sendSimpleEmail(recipient.getEmail(), "PrestArte · " + title,
                    "Hola " + recipient.getName() + ",\n\n" + message + url + "\n\nEl equipo de Prestarte.");
        }
    }

    /**
     * Avisa a todas las partes de un préstamo (coleccionista, fundación y
     * transportistas) salvo a quien provocó el hito.
     */
    @Transactional
    public void notifyLoanParties(LoanRequest loan, String message, boolean email) {
        Long actorId = currentUser.currentIdOrNull();
        String title = "Préstamo de «" + loan.getArtwork().getTitle() + "»";
        String link = "/app/loans/" + loan.getId();
        for (User u : parties(loan).values()) {
            if (!u.getId().equals(actorId)) {
                notify(u, title, message, link, email);
            }
        }
    }

    /** Coleccionista, fundación y transportistas del préstamo, sin repetidos. */
    public Map<Long, User> parties(LoanRequest loan) {
        Map<Long, User> users = new LinkedHashMap<>();
        users.put(loan.getArtwork().getCollector().getId(), loan.getArtwork().getCollector());
        users.put(loan.getFoundation().getId(), loan.getFoundation());
        for (Shipment s : shipmentRepository.findAllByLoanRequestId(loan.getId())) {
            users.putIfAbsent(s.getTransportCompany().getId(), s.getTransportCompany());
        }
        return users;
    }

    // ===== Consulta del usuario con sesión =====

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine() {
        return notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDescIdDesc(currentUser.currentId())
                .stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .link(n.getLink())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByRecipientIdAndReadFalse(currentUser.currentId());
    }

    @Transactional
    public void markRead(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Aviso", id));
        if (!n.getRecipient().getId().equals(currentUser.currentId())) {
            throw new AccessDeniedException("Este aviso no es tuyo");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead(currentUser.currentId());
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
