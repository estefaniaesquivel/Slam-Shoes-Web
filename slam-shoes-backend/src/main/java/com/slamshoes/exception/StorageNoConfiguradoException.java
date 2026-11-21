package com.slamshoes.exception;

public class StorageNoConfiguradoException extends RuntimeException {
    public StorageNoConfiguradoException() {
        super("Falta configurar la llave de Supabase en el backend (application-secrets.properties) para subir imágenes");
    }
}
