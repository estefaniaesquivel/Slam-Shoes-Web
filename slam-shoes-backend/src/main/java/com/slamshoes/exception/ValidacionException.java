package com.slamshoes.exception;

// REFACTOR: Replace Error Code with Exception
// RAZON: Antes las validaciones devolvían un String con el problema (validar() en novedades y ofertas) o lanzaban
// IllegalArgumentException, y cada controlador los convertía con error(String) en un 400. Ahora los servicios lanzan
// ValidacionException y GlobalExceptionHandler responde HTTP 400 con {"error": mensaje}.
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
