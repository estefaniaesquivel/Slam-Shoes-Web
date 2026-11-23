package com.slamshoes.exception;

// REFACTOR: Replace Error Code with Exception
// RAZON: Antes cada controlador Admin* repetía un método privado sinLlave() que devolvía un 500 con el mensaje de
// "falta configurar la llave de Supabase". Ahora el servicio lanza esta excepción (el mensaje vive solo aquí) y
// GlobalExceptionHandler responde HTTP 500; se eliminó la copia duplicada de cada controlador.
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
