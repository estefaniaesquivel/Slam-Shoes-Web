package com.slamshoes.exception;

// REFACTOR: Replace Error Code with Exception
// RAZON: Antes un fallo al subir a Supabase se capturaba en cada controlador (catch Exception) y se devolvía un
// ResponseEntity 502 con el mensaje armado a mano. Ahora ProductoAdminService y AdminCatalogoService lanzan
// StorageException (conservando la causa IOException) y GlobalExceptionHandler responde HTTP 502 una sola vez.
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
