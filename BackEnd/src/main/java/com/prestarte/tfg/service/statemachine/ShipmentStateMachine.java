package com.prestarte.tfg.service.statemachine;

import com.prestarte.tfg.model.entity.Shipment.ShipmentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Tabla de transiciones permitidas para Shipment.ShipmentStatus.
 *
 *   REQUESTED ─┬→ QUOTED ─┬→ APPROVED ─→ PICKED_UP ─→ IN_TRANSIT ─→ DELIVERED
 *              │          └→ REJECTED
 *              └→ REJECTED
 *
 *   Estados terminales (sin salida): REJECTED, DELIVERED.
 *   El retorno se modela como un Shipment NUEVO con direction = RETURN, no como
 *   transición del shipment original.
 */
@Component
public class ShipmentStateMachine {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(ShipmentStatus.class);
        ALLOWED.put(ShipmentStatus.REQUESTED,  EnumSet.of(ShipmentStatus.QUOTED, ShipmentStatus.REJECTED));
        ALLOWED.put(ShipmentStatus.QUOTED,     EnumSet.of(ShipmentStatus.APPROVED, ShipmentStatus.REJECTED));
        ALLOWED.put(ShipmentStatus.APPROVED,   EnumSet.of(ShipmentStatus.PICKED_UP));
        ALLOWED.put(ShipmentStatus.PICKED_UP,  EnumSet.of(ShipmentStatus.IN_TRANSIT));
        ALLOWED.put(ShipmentStatus.IN_TRANSIT, EnumSet.of(ShipmentStatus.DELIVERED));
        // Estados terminales
        ALLOWED.put(ShipmentStatus.DELIVERED, EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.REJECTED,  EnumSet.noneOf(ShipmentStatus.class));
    }

    public void validate(ShipmentStatus from, ShipmentStatus to) {
        if (from == null) {
            throw new IllegalStateException("El envío no tiene estado inicial.");
        }
        Set<ShipmentStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ShipmentStatus.class));
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                    "Transición de envío no permitida: " + from + " → " + to + ". " +
                    "Transiciones válidas desde " + from + ": " + allowed);
        }
    }

    public boolean canTransition(ShipmentStatus from, ShipmentStatus to) {
        Set<ShipmentStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ShipmentStatus.class));
        return allowed.contains(to);
    }
}
