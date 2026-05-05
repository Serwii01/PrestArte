package com.prestarte.tfg.exception;

/**
 * Lanzada cuando se intenta registrar un email ya existente.
 * Mapea a HTTP 409 Conflict en GlobalExceptionHandler.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Ya existe una cuenta con el email: " + email);
    }
}
