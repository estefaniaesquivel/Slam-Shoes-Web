package com.slamshoes.exception;

/*REFACTOR: Replace Error Code with Exception
RAZON: Un pedido sin productos se representaba con un IllegalArgumentException genérico, indistinguible de otros parámetros inválidos. Esta excepción tipada permite responderlo como HTTP 400 desde GlobalExceptionHandler.
*/
/**
 * Excepción lanzada cuando se intenta crear un pedido sin productos.
 */
public class PedidoVacioException extends IllegalArgumentException {

    public PedidoVacioException(String mensaje) {
        super(mensaje);
    }
}
