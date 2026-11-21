package com.slamshoes.exception;

public class StorageException extends RuntimeException {
    public StorageException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public StorageException(String mensaje) {
        super(mensaje);
    }
}
