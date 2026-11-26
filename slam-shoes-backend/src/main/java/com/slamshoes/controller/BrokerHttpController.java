package com.slamshoes.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.http.Json;
import com.slamshoes.broker.http.Marshalling;
import com.slamshoes.broker.http.MensajeHttp;
import com.slamshoes.broker.http.NucleoBroker;
import com.slamshoes.broker.http.RutasBroker;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Puerta HTTP del Broker (lado Broker). Sustituye al {@code ServerSocket} que escuchaba en el puerto 9000:
 * ahora el Broker recibe todo por los endpoints REST de la aplicación (puerto {@code $PORT}, HTTPS de Render).
 *
 * <pre>
 *  POST /api/broker/register      registro dinámico de un proveedor   {"servicio","url",...}
 *  POST /api/broker/unregister    baja de un proveedor
 *  GET  /api/broker/health        health check para Render
 *  POST /api/broker/solicitar     envoltura Solicitud/Respuesta (la usa BrokerGateway)
 *  ANY  /api/gateway/{servicio}/** reenvío transparente al proveedor (ej. GET /api/gateway/productos)
 * </pre>
 *
 * Solo traduce HTTP a llamadas de {@link NucleoBroker}; no decide nada. Se activa con
 * {@code broker.servidor.activo=true} (variable de entorno BROKER_SERVIDOR_ACTIVO, verdadero por omisión).
 *
 * REFACTOR: Replace Conditional with Polymorphism (aquí, reemplazar el "switch" por tipo de mensaje por un endpoint por tipo)
 * RAZÓN: el servidor de sockets recibía todo por una sola conexión y decidía con un {@code switch} sobre el campo "tipo"
 * (SOLICITUD, REGISTRAR, BAJA, ESTADO). Ahora cada tipo de mensaje tiene su propia ruta y método, de modo que el
 * enrutamiento lo hace Spring MVC, cada método tiene una sola tarea y agregar una operación nueva no obliga a modificar
 * un bloque central.
 */
@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "broker.servidor.activo", havingValue = "true", matchIfMissing = true)
public class BrokerHttpController {

    private static final String JSON = "application/json";
    private static final int MAXIMO_CUERPO = 1_000_000;

    private final NucleoBroker nucleo;

    public BrokerHttpController(NucleoBroker nucleo) {
        this.nucleo = nucleo;
    }

    /** Health check ligero: Render lo consulta para saber que el contenedor está activo. */
    @GetMapping("/broker/health")
    public ResponseEntity<String> salud() {
        return json(200, Json.escribir(nucleo.salud()));
    }

    /** Registro dinámico de un proveedor. */
    @PostMapping("/broker/register")
    // REFACTOR: Split Phase
    // RAZON: el servidor de sockets mezclaba leer la linea, decidir y escribir la respuesta en un solo metodo. El
    // controlador solo traduce: fase 1 convierte el cuerpo JSON en mapa (400 si esta mal formado) y fase 2 delega
    // la decision en {@code NucleoBroker.registrar}, cuyo Acuse se convierte en codigo HTTP.
    public ResponseEntity<String> registrar(@RequestHeader(value = RutasBroker.CABECERA_TOKEN, required = false) String token,
                                            @RequestBody(required = false) String cuerpo,
                                            HttpServletRequest peticion) {
        Map<String, Object> mensaje;
        try {
            mensaje = Marshalling.leerMensaje(cuerpo);
        } catch (IllegalArgumentException e) {
            return json(400, Json.escribir(new NucleoBroker.Acuse(400, false, "Registro rechazado: " + e.getMessage()).aMapa()));
        }
        NucleoBroker.Acuse acuse = nucleo.registrar(token, mensaje, peticion.getRemoteAddr());
        return json(acuse.codigo(), Json.escribir(acuse.aMapa()));
    }

    /** Baja de un proveedor. */
    @PostMapping("/broker/unregister")
    // REFACTOR: Split Phase
    // RAZON: igual que el registro: el controlador solo lee el JSON y traduce el Acuse a HTTP; quien decide si la
    // baja procede es NucleoBroker.
    public ResponseEntity<String> darDeBaja(@RequestHeader(value = RutasBroker.CABECERA_TOKEN, required = false) String token,
                                            @RequestBody(required = false) String cuerpo,
                                            HttpServletRequest peticion) {
        Map<String, Object> mensaje;
        try {
            mensaje = Marshalling.leerMensaje(cuerpo);
        } catch (IllegalArgumentException e) {
            return json(400, Json.escribir(new NucleoBroker.Acuse(400, false, "Baja rechazada: " + e.getMessage()).aMapa()));
        }
        NucleoBroker.Acuse acuse = nucleo.darDeBaja(token, mensaje, peticion.getRemoteAddr());
        return json(acuse.codigo(), Json.escribir(acuse.aMapa()));
    }

    /** Recibe una Solicitud completa y devuelve SIEMPRE una Respuesta (HTTP 200; el resultado va en "estado"). */
    @PostMapping("/broker/solicitar")
    // REFACTOR: Change Function Declaration
    // RAZON: antes el tipo SOLICITUD llegaba por el mismo socket que los demas mensajes y el switch devolvia un
    // String. Ahora es un metodo con su ruta ({@code POST /api/broker/solicitar}) que devuelve siempre HTTP 200
    // con una Respuesta, y el campo estado indica el resultado (contrato que ya usaba BrokerGateway).
    public ResponseEntity<String> solicitar(@RequestBody(required = false) String cuerpo, HttpServletRequest peticion) {
        Respuesta respuesta;
        try {
            respuesta = nucleo.solicitar(Marshalling.leerMensaje(cuerpo), peticion.getRemoteAddr());
        } catch (IllegalArgumentException e) {
            respuesta = Respuesta.error("sin-id", com.slamshoes.broker.EstadoRespuesta.SOLICITUD_INVALIDA,
                    "Solicitud mal formada: " + e.getMessage());
        }
        return json(200, Marshalling.marshalRespuesta(respuesta));
    }

    /** Reenvío transparente cuando la ruta no trae nada después del nombre del servicio (GET /api/gateway/productos). */
    @RequestMapping("/gateway/{servicio}")
    public ResponseEntity<String> proxyRaiz(@PathVariable String servicio, HttpServletRequest peticion) {
        return reenviar(servicio, peticion);
    }

    /** Reenvío transparente: GET /api/gateway/productos/5 llega al proveedor como GET {url}{rutaBase}/5. */
    @RequestMapping("/gateway/{servicio}/**")
    public ResponseEntity<String> proxy(@PathVariable String servicio, HttpServletRequest peticion) {
        return reenviar(servicio, peticion);
    }

    // REFACTOR: Extract Function
    // RAZON: las rutas {@code /gateway/{servicio}} y {@code /gateway/{servicio}/**} hacen lo mismo (calcular el
    // resto de la ruta, leer el cuerpo y reenviar); se extrajo a un metodo privado para no duplicar ese codigo en
    // los dos manejadores.
    private ResponseEntity<String> reenviar(String servicio, HttpServletRequest peticion) {
        String uri = peticion.getRequestURI();
        String prefijo = RutasBroker.PROXY + "/" + servicio;
        int inicio = uri.indexOf(prefijo);
        String resto = inicio < 0 ? "" : uri.substring(inicio + prefijo.length());

        String cuerpo = null;
        String metodo = peticion.getMethod();
        if (!metodo.equals("GET") && !metodo.equals("DELETE")) {
            try {
                cuerpo = new String(peticion.getInputStream().readNBytes(MAXIMO_CUERPO), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return json(400, "{\"estado\":\"SOLICITUD_INVALIDA\",\"error\":\"No se pudo leer el cuerpo\"}");
            }
        }
        MensajeHttp respuesta = nucleo.getProxy().reenviar(servicio, metodo, resto, peticion.getQueryString(),
                cuerpo, peticion.getHeader("Content-Type"), peticion.getHeader("Accept"));
        return ResponseEntity.status(respuesta.codigo())
                .header("Content-Type", respuesta.tipoContenido() == null ? JSON : respuesta.tipoContenido())
                .body(respuesta.cuerpo());
    }

    // REFACTOR: Extract Function
    // RAZON: armar ResponseEntity con codigo y Content-Type JSON se repetia en cada endpoint; un metodo unico
    // mantiene el mismo formato.
    private static ResponseEntity<String> json(int codigo, String cuerpo) {
        return ResponseEntity.status(codigo).header("Content-Type", JSON).body(cuerpo);
    }
}
