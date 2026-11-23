package com.slamshoes.exception;

// REFACTOR: Replace Error Code with Exception
// RAZON: Antes el conflicto "el producto ya aparece en pedidos" se resolvía en el controlador con un código HTTP armado a
// mano. Ahora ProductoAdminService.eliminar lanza esta excepción y GlobalExceptionHandler la convierte en HTTP 409. Nota:
// eliminarJugador/eliminarMarca siguen desvinculando productos en lugar de lanzarla.
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
