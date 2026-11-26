package com.slamshoes.exception;

/*REFACTOR: Replace Error Code with Exception
RAZON: cambiarEstado devolvía una respuesta CONFLICTO cuando el pedido no estaba en el estado esperado. Una excepción tipada permite que el modelo haga cumplir la transición y que GlobalExceptionHandler responda HTTP 409.
*/
/**
 * Excepción lanzada cuando un pedido no puede realizar la transición de estado solicitada.
 */
public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
