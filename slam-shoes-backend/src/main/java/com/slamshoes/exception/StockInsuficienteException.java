package com.slamshoes.exception;

/*REFACTOR: Replace Error Code with Exception
RAZON: El stock insuficiente se devolvía como una respuesta CONFLICTO genérica. Esta excepción tipada permite que el error atraviese el Broker y que GlobalExceptionHandler responda HTTP 409; al propagarse fuera del método transaccional, la operación se revierte.
*/
/**
 * Excepción lanzada cuando no hay existencias suficientes para completar un pedido.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
