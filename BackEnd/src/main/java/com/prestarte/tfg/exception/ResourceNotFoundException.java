package com.prestarte.tfg.exception;

/**
 * Excepción que se lanza cuando una entidad solicitada no existe en
 * la base de datos.
 *
 * El manejador global de errores la traduce a un HTTP 404. La fábrica
 * estática {@link #of(String, Object)} compone un mensaje uniforme
 * indicando el tipo de entidad y el identificador buscado.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Construye la excepción con el mensaje estándar para una entidad e identificador. */
    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException(entity + " no encontrado con id: " + id);
    }
}
