package com.slamshoes.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.http.ExposicionProveedores;
import com.slamshoes.broker.http.Json;
import com.slamshoes.broker.http.Marshalling;
import com.slamshoes.broker.http.RutasBroker;

/**
 * Puerta HTTP de los PROVEEDORES (lado microservicio): {@code POST /api/proveedor/{servicio}} recibe la solicitud que
 * el Broker reenvía, la ejecuta en el proveedor y contesta la Respuesta en JSON. Sustituye al {@code ServerSocket}
 * que cada proveedor abría en su propio puerto (9101, 9102, 9103).
 *
 * Además de este endpoint de intercambio entre Broker y proveedor, las operaciones también quedan disponibles como
 * REST estándar en los {@code @RestController} existentes (/api/productos, /api/pedidos, /api/usuarios), que son los
 * que alcanza el proxy {@code /api/gateway/{servicio}/**} del Broker.
 *
 * Se activa con {@code broker.proveedores.activo=true} (variable BROKER_PROVEEDORES_ACTIVO, verdadero por omisión).
 *
 * REFACTOR: Change Function Declaration (de "escuchar un puerto" a "atender una petición")
 * RAZÓN: el proveedor antiguo exponía un servidor completo con hilos propios; ahora expone un único método con
 * firma {@code atender(servicio, json)}, que Spring invoca por cada petición. Menos código propio, menos hilos que
 * administrar y compatible con cualquier proxy HTTP.
 */
@RestController
@RequestMapping(RutasBroker.PROVEEDOR)
@ConditionalOnProperty(name = "broker.proveedores.activo", havingValue = "true", matchIfMissing = true)
public class ProveedorHttpController {

    private static final String JSON = "application/json";

    private final ExposicionProveedores exposicion;

    public ProveedorHttpController(ExposicionProveedores exposicion) {
        this.exposicion = exposicion;
    }

    /** Estado ligero de los proveedores alojados en esta aplicación. */
    @GetMapping("/health")
    public ResponseEntity<String> salud() {
        return ResponseEntity.status(200).header("Content-Type", JSON).body(Json.escribir(exposicion.salud()));
    }

    /** Recibe la Solicitud (JSON) del Broker y devuelve la Respuesta (JSON). */
    @PostMapping("/{servicio}")
    public ResponseEntity<String> atender(@PathVariable String servicio,
                                          @RequestHeader(value = RutasBroker.CABECERA_TOKEN, required = false) String token,
                                          @RequestBody(required = false) String cuerpo) {
        if (!exposicion.autorizado(token)) {
            Respuesta denegada = Respuesta.error("sin-id", EstadoRespuesta.NO_AUTORIZADO,
                    "Falta o es incorrecto el token del Broker");
            return ResponseEntity.status(401).header("Content-Type", JSON).body(Marshalling.marshalRespuesta(denegada));
        }
        Respuesta respuesta = exposicion.atender(servicio, cuerpo == null ? "" : cuerpo);
        return ResponseEntity.status(200).header("Content-Type", JSON).body(Marshalling.marshalRespuesta(respuesta));
    }
}
