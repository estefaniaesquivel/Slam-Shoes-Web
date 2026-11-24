package com.slamshoes.exception;

// REFACTOR: Replace Error Code with Exception
// RAZON: Antes un registro inexistente se señalaba con ResponseEntity.notFound() en cada controlador o con
// IllegalArgumentException("La marca no existe") que el controlador traducía a 404. Ahora los servicios lanzan esta
// excepción con orElseThrow y GlobalExceptionHandler responde HTTP 404 en un solo lugar.
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
