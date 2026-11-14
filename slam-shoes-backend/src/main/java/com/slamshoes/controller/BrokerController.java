package com.slamshoes.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.service.BrokerGateway;

/**
 * Puerta HTTP unica hacia el Broker para el frontend (Vue).
 *
 * El navegador solo necesita saber "que servicio, que operacion y con que parametros":
 *   POST /api/broker/{servicio}/{operacion}   (cuerpo JSON = parametros)
 * No conoce tablas, repositorios ni quien atiende cada servicio.
 *
 * Opcional: ?idSolicitud=... permite al navegador elegir el identificador. Si repite una solicitud
 * con el mismo id (doble clic, reintento de red), el Broker devuelve la respuesta anterior en lugar
 * de ejecutar la operacion otra vez (por ejemplo, crear dos pedidos).
 */
@RestController
@RequestMapping("/api/broker")
@CrossOrigin(origins = "*")
public class BrokerController {

    private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9_-]{8,100}");

    @Autowired
    private BrokerGateway gateway;

    @PostMapping("/{servicio}/{operacion}")
    public ResponseEntity<Map<String, Object>> enviar(@PathVariable String servicio,
                                                      @PathVariable String operacion,
                                                      @RequestParam(required = false) String idSolicitud,
                                                      @RequestBody(required = false) Map<String, Object> parametros) {
        Map<String, Object> params = parametros == null ? new LinkedHashMap<>() : parametros;

        Solicitud solicitud;
        try {
            if (idSolicitud != null && !idSolicitud.isBlank()) {
                if (!ID_VALIDO.matcher(idSolicitud).matches()) {
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "idSolicitud invalido: usa de 8 a 100 letras, numeros, guion o guion bajo",
                            "estado", EstadoRespuesta.SOLICITUD_INVALIDA.name()));
                }
                solicitud = Solicitud.reconstruir(idSolicitud, servicio, operacion, params, LocalDateTime.now());
            } else {
                solicitud = new Solicitud(servicio, operacion, params);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(),
                    "estado", EstadoRespuesta.SOLICITUD_INVALIDA.name()));
        }

        Respuesta respuesta = gateway.enviar(solicitud);
        if (!respuesta.isExitosa()) {
            return RespuestaHttp.error(respuesta);
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idSolicitud", respuesta.getIdSolicitud());
        cuerpo.put("estado", respuesta.getEstado().name());
        cuerpo.put("resultado", respuesta.getResultado());
        cuerpo.put("fechaHora", respuesta.getFechaHora());
        return ResponseEntity.ok(cuerpo);
    }

    /** Monitoreo: servicios registrados, metricas y ultimas solicitudes de la bitacora. */
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        try {
            return ResponseEntity.ok(gateway.estado());
        } catch (IOException e) {
            return ResponseEntity.status(503).body(Map.of("error", "El Broker no esta disponible: " + e.getMessage(),
                    "estado", EstadoRespuesta.BROKER_NO_DISPONIBLE.name()));
        }
    }
}
