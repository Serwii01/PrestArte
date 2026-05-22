package com.prestarte.tfg.security;

import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Punto de acceso al usuario autenticado desde la capa de servicios.
 *
 * Centraliza la lectura del {@code SecurityContext} y ofrece utilidades
 * para comprobar la identidad del usuario que está ejecutando una
 * acción concreta. Cuando una operación solo puede realizarla el dueño
 * de un recurso o un administrador, los servicios delegan en esta
 * clase y dejan que se lance {@link AccessDeniedException} si la
 * verificación falla; el manejador global de errores se encarga
 * después de devolver el HTTP 403 correspondiente.
 */
@Service
public class CurrentUser {

    /**
     * Devuelve el usuario asociado a la sesión actual. Si no hay
     * sesión o no se reconoce el principal, lanza {@code AccessDeniedException}.
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

    /** Atajo para obtener el identificador del usuario autenticado. */
    public Long currentId() {
        return getOrThrow().getId();
    }

    /**
     * Variante tolerante que devuelve el identificador del usuario
     * autenticado o {@code null} si no hay sesión. Resulta práctica en
     * endpoints públicos que necesitan saber quién está mirando para
     * filtrar campos sensibles sin obligar a iniciar sesión.
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

    /** Indica si la sesión actual pertenece a un administrador, o {@code false} si no hay sesión. */
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
     * Exige que el usuario actual coincida con el identificador
     * indicado; si no es así, lanza una excepción de acceso denegado.
     * Pensado para acciones que solo puede ejecutar el dueño del
     * recurso.
     */
    public void requireUserId(Long expected) {
        if (expected == null || !expected.equals(currentId())) {
            throw new AccessDeniedException("No tienes permiso para esta operación");
        }
    }

    /**
     * Exige que el usuario actual sea alguno de los identificadores
     * indicados. Resulta útil cuando la acción la pueden realizar
     * varias partes, por ejemplo cancelar un préstamo (coleccionista
     * o fundación).
     */
    public void requireAnyUserId(Long... allowed) {
        Long me = currentId();
        for (Long id : allowed) {
            if (id != null && id.equals(me)) return;
        }
        throw new AccessDeniedException("No tienes permiso para esta operación");
    }

    /** Indica si la sesión actual tiene rol de administrador. */
    public boolean isAdmin() {
        return getOrThrow().getRole() == Role.ADMIN;
    }

    /** Indica si el usuario actual coincide con alguno de los identificadores indicados. */
    public boolean isAnyOf(Long... allowed) {
        Long me = currentId();
        for (Long id : allowed) {
            if (id != null && id.equals(me)) return true;
        }
        return false;
    }
}
