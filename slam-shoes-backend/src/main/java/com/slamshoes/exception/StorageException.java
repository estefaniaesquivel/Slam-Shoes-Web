package com.slamshoes.exception;

/**
 * Excepción lanzada ante fallos de comunicación o respuesta inválida del servicio de almacenamiento.
 */
public class StorageException extends RuntimeException {

    /**
     * Crea la excepción conservando el mensaje y la causa original del fallo.
     *
     * @param mensaje descripción del error en el almacenamiento
     * @param causa excepción subyacente de red o E/S
     */
    public StorageException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @param mensaje descripción del error en el almacenamiento
     */
    public StorageException(String mensaje) {
        super(mensaje);
    }
}
