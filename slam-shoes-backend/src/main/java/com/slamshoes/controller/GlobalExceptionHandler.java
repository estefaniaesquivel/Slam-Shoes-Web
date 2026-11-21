package com.slamshoes.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.slamshoes.exception.ConflictoEliminacionException;
import com.slamshoes.exception.RecursoNoEncontradoException;
import com.slamshoes.exception.StorageException;
import com.slamshoes.exception.StorageNoConfiguradoException;
import com.slamshoes.exception.ValidacionException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(ValidacionException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ConflictoEliminacionException.class)
    public ResponseEntity<Map<String, String>> manejarConflicto(ConflictoEliminacionException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(StorageNoConfiguradoException.class)
    public ResponseEntity<Map<String, String>> manejarStorageNoConfigurado(StorageNoConfiguradoException ex) {
        return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, String>> manejarStorage(StorageException ex) {
        return ResponseEntity.status(502).body(Map.of("error", ex.getMessage()));
    }
}
