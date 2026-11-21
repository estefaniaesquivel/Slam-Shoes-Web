package com.slamshoes.exception;

/**
 * Excepción lanzada cuando el servicio de almacenamiento externo no tiene credenciales configuradas.
 */
public class StorageNoConfiguradoException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje estándar de configuración faltante en Supabase.
     */
    public StorageNoConfiguradoException() {
        super("Falta configurar la llave de Supabase en el backend (application-secrets.properties) para subir imágenes");
    }
}
