package com.prestarte.tfg.security;

import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Acceso al usuario autenticado dentro de la capa de servicios.
 *
 * Cualquier acción del flujo (aceptar préstamo, subir presupuesto, etc.) debe
 * llamar a {@link #requireUserId(Long)} o {@link #getOrThrow()} para verificar
 * que quien ejecuta la acción es quien debería. Si no, lanzamos
 * {@link AccessDeniedException} (mapeada a 403 por GlobalExceptionHandler).
 */
@Service
public class CurrentUser {

    /**
     * Devuelve el {@link User} autenticado, o lanza {@link AccessDeniedException}.
     */
    public User getOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Necesitas iniciar sesión para esta acción");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUser();
        }
        throw new AccessDeniedException("Sesión no reconocida");
    }

    /** Atajo: id del usuario actual. */
    public Long currentId() {
        return getOrThrow().getId();
    }

    /**
     * Versión "blanda": devuelve el id del usuario autenticado o {@code null}
     * si no hay sesión. Útil para endpoints públicos que necesitan saber
     * quién mira para filtrar campos sensibles sin obligar a iniciar sesión.
     */
    public Long currentIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUser().getId();
        }
        return null;
    }

    /** True si hay sesión y el usuario tiene rol ADMIN. */
    public boolean isAdminOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUser().getRole() == Role.ADMIN;
        }
        return false;
    }

    /**
     * Verifica que el usuario actual es exactamente el id esperado. Si no,
     * lanza 403. Pensado para "esta acción solo la puede hacer el dueño X".
     */
    public void requireUserId(Long expected) {
        if (expected == null || !expected.equals(currentId())) {
            throw new AccessDeniedException("No tienes permiso para esta operación");
        }
    }

    /**
     * Verifica que el usuario actual es uno de los ids permitidos. Útil cuando
     * la acción la pueden hacer dos partes (ej. cancelar = collector o foundation).
     */
    public void requireAnyUserId(Long... allowed) {
        Long me = currentId();
        for (Long id : allowed) {
            if (id != null && id.equals(me)) return;
        }
        throw new AccessDeniedException("No tienes permiso para esta operación");
    }

    /** True si el usuario actual tiene rol ADMIN. */
    public boolean isAdmin() {
        return getOrThrow().getRole() == Role.ADMIN;
    }

    /** True si el usuario actual coincide con alguno de los ids pasados. */
    public boolean isAnyOf(Long... allowed) {
        Long me = currentId();
        for (Long id : allowed) {
            if (id != null && id.equals(me)) return true;
        }
        return false;
    }
}
