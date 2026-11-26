package com.slamshoes.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.slamshoes.exception.ConflictoEliminacionException;
import com.slamshoes.exception.PedidoVacioException;
import com.slamshoes.exception.RecursoNoEncontradoException;
import com.slamshoes.exception.StorageException;
import com.slamshoes.exception.StorageNoConfiguradoException;
import com.slamshoes.exception.StockInsuficienteException;
import com.slamshoes.exception.ValidacionException;

// REFACTOR: Extract Class
// RAZON: Antes cada controlador Admin* repetia los metodos privados error(String) y sinLlave() y armaba a mano los ResponseEntity 400/404/409/500/502. Ahora se centralizan en este @RestControllerAdvice.
/**
 * Manejador global de excepciones REST que traduce errores de dominio a respuestas JSON uniformes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Responde HTTP 400 cuando fallan las reglas de validación de entrada.
     *
     * @param ex excepción de validación capturada
     * @return respuesta con código 400 y cuerpo {@code {"error": mensaje}}
     */
    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(ValidacionException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * Responde HTTP 400 cuando se intenta crear un pedido sin productos.
     *
     * @param ex excepción de pedido vacío
     * @return respuesta con código 400 y cuerpo {@code {"error": mensaje}}
     */
    @ExceptionHandler(PedidoVacioException.class)
    public ResponseEntity<Map<String, String>> manejarPedidoVacio(PedidoVacioException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * Responde HTTP 404 cuando el recurso solicitado no existe.
     *
     * @param ex excepción de recurso no encontrado
     * @return respuesta con código 404 y cuerpo {@code {"error": mensaje}}
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Responde HTTP 409 cuando existe un conflicto de integridad al eliminar.
     *
     * @param ex excepción de conflicto de eliminación
     * @return respuesta con código 409 y cuerpo {@code {"error": mensaje}}
     */
    @ExceptionHandler(ConflictoEliminacionException.class)
    public ResponseEntity<Map<String, String>> manejarConflicto(ConflictoEliminacionException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Responde HTTP 409 cuando no hay existencias suficientes para completar un pedido.
     *
     * @param ex excepción de stock insuficiente
     * @return respuesta con código 409 y cuerpo {@code {"error": mensaje}}
     */
    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> manejarStockInsuficiente(StockInsuficienteException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Responde HTTP 500 cuando faltan credenciales de almacenamiento externo.
     *
     * @param ex excepción de configuración faltante
     * @return respuesta con código 500 y cuerpo {@code {"error": mensaje}}
     */
    @ExceptionHandler(StorageNoConfiguradoException.class)
    public ResponseEntity<Map<String, String>> manejarStorageNoConfigurado(StorageNoConfiguradoException ex) {
        return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Responde HTTP 502 cuando falla la comunicación con el almacenamiento externo.
     *
     * @param ex excepción de almacenamiento
     * @return respuesta con código 502 y cuerpo {@code {"error": mensaje}}
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, String>> manejarStorage(StorageException ex) {
        return ResponseEntity.status(502).body(Map.of("error", ex.getMessage()));
    }
}
