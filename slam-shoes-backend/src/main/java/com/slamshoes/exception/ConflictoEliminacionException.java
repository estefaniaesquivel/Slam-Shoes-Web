package com.slamshoes.exception;

/**
 * Excepción lanzada cuando no es posible eliminar una entidad por dependencias activas.
 */
public class ConflictoEliminacionException extends RuntimeException {

    /**
     * Crea la excepción con el motivo del conflicto de integridad.
     *
     * @param mensaje explicación del conflicto que impide la eliminación
     */
    public ConflictoEliminacionException(String mensaje) {
        super(mensaje);
    }
}
