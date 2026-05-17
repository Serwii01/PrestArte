package com.prestarte.tfg.service.statemachine;

import com.prestarte.tfg.model.entity.LoanRequest.Status;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class LoanStateMachine {

    private static final Map<Status, Set<Status>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(Status.class);
        ALLOWED.put(Status.REQUESTED,        EnumSet.of(Status.ACCEPTED, Status.REJECTED, Status.CANCELLED));
        ALLOWED.put(Status.ACCEPTED,         EnumSet.of(Status.QUOTE_PENDING, Status.CANCELLED));
        ALLOWED.put(Status.QUOTE_PENDING,    EnumSet.of(Status.QUOTE_PROPOSED, Status.CANCELLED));
        ALLOWED.put(Status.QUOTE_PROPOSED,   EnumSet.of(Status.PAID, Status.QUOTE_PENDING, Status.CANCELLED));
        ALLOWED.put(Status.PAID,             EnumSet.of(Status.READY_FOR_PICKUP, Status.CANCELLED));
        ALLOWED.put(Status.READY_FOR_PICKUP, EnumSet.of(Status.IN_TRANSIT, Status.CANCELLED));
        ALLOWED.put(Status.IN_TRANSIT,       EnumSet.of(Status.DELIVERED));
        ALLOWED.put(Status.DELIVERED,        EnumSet.of(Status.ON_LOAN));
        ALLOWED.put(Status.ON_LOAN,          EnumSet.of(Status.RETURNING));
        ALLOWED.put(Status.RETURNING,        EnumSet.of(Status.RETURNED));
        // Estados terminales
        ALLOWED.put(Status.RETURNED,  EnumSet.noneOf(Status.class));
        ALLOWED.put(Status.REJECTED,  EnumSet.noneOf(Status.class));
        ALLOWED.put(Status.CANCELLED, EnumSet.noneOf(Status.class));
    }

    /**
     * Verifica que la transición {@code from → to} es válida.
     * Si no lo es, lanza {@link IllegalStateException} (mapeada a HTTP 409 por GlobalExceptionHandler).
     */
    public void validate(Status from, Status to) {
        if (from == null) {
            throw new IllegalStateException("El préstamo no tiene estado inicial.");
        }
        Set<Status> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(Status.class));
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                    "Transición de préstamo no permitida: " + from + " → " + to + ". " +
                    "Transiciones válidas desde " + from + ": " + allowed);
        }
    }

    public boolean canTransition(Status from, Status to) {
        Set<Status> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(Status.class));
        return allowed.contains(to);
    }
}
