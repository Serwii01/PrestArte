package com.prestarte.tfg.exception;

/**
 * Lanzada cuando una entidad no existe en la base de datos.
 * Mapea a HTTP 404 Not Found en GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException(entity + " no encontrado con id: " + id);
    }
}
