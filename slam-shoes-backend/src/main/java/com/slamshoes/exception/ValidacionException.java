package com.slamshoes.exception;

/**
 * Excepción de negocio lanzada cuando los datos de entrada no cumplen las reglas de validación.
 */
public class ValidacionException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje descriptivo del error de validación.
     *
     * @param mensaje descripción del campo o regla incumplida
     */
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
