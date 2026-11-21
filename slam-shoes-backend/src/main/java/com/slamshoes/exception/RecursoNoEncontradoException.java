package com.slamshoes.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no existe en la base de datos.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * Crea la excepción indicando el recurso faltante.
     *
     * @param mensaje descripción del recurso no encontrado
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
