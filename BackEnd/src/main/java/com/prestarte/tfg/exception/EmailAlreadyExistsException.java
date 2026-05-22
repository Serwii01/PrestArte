package com.prestarte.tfg.exception;

/**
 * Excepción que se lanza cuando se intenta crear una cuenta con un
 * correo electrónico que ya está registrado.
 *
 * El manejador global de errores la traduce a un HTTP 409 (conflicto)
 * para que el cliente pueda mostrar un mensaje específico.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Ya existe una cuenta con el email: " + email);
    }
}
