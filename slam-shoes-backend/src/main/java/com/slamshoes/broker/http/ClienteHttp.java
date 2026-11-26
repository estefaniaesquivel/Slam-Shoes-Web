package com.slamshoes.broker.http;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import com.slamshoes.broker.Broker;
import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * Implementación de {@link Broker} que habla con un Broker REMOTO por HTTP/HTTPS ({@code BROKER_URL}).
 *
 * Es la "otra implementación" que el Nivel 1 dejó prevista: {@link com.slamshoes.broker.cliente.ClienteBroker}
 * y {@code BrokerGateway} solo conocen la interfaz Broker, así que pasaron de sockets a HTTP SIN cambiar una línea.
 *
 * Nunca lanza excepciones: si el Broker está apagado, dormido o no contesta a tiempo devuelve una Respuesta con
 * estado BROKER_NO_DISPONIBLE o TIEMPO_AGOTADO. Los reintentos (limitados) usan el MISMO idSolicitud y el Broker
 * descarta los duplicados.
 *
 * REFACTOR: Substitute Algorithm (la implementación de Broker que habla con el exterior pasa de sockets a HTTP)
 * RAZÓN: antes la aplicación dependía de {@code ClienteSocket}, atado a host:puerto TCP. Con la interfaz {@link Broker}
 * como frontera, cambiar de transporte es cambiar de implementación y no tocar al cliente, a los controladores ni
 * a BrokerCentral, o sea, el cambio se refleja en toda la clase.
 */
public class ClienteHttp implements Broker {

    private final URI urlBroker;
    private final int msConexion;
    private final int msLectura;
    private final int reintentos;

    public ClienteHttp(String urlBroker) {
        this(urlBroker, 2000, 20_000, 1);
    }

    /**
     * @param urlBroker  URL base del Broker, por ejemplo {@code https://slam-broker.onrender.com}
     * @param msConexion tiempo máximo para conectarse
     * @param msLectura  tiempo máximo esperando la respuesta (en Render, un servicio dormido tarda en despertar)
     * @param reintentos reintentos limitados tras un fallo de red o un 502/503/504
     * @throws IllegalArgumentException si la URL no es http/https válida
     */
    // REFACTOR: Replace Primitive with Object
    // RAZON: {@code ClienteSocket} recibia host (String) y puerto (int) por separado, sin validar. Ahora recibe
    // una sola URL que se convierte en un {@link URI} validado (solo http/https, sin credenciales ni barra final),
    // asi un valor mal escrito de BROKER_URL falla al arrancar con un mensaje claro y no al primer intento de
    // conexion.
    public ClienteHttp(String urlBroker, int msConexion, int msLectura, int reintentos) {
        this.urlBroker = RegistroProveedor.urlBase(urlBroker);
        this.msConexion = msConexion;
        this.msLectura = msLectura;
        this.reintentos = Math.max(0, reintentos);
    }

    @Override
    // REFACTOR: Decompose Conditional
    // RAZON: la decision 'que hago con lo que contesto el Broker' dependia de si la linea del socket se pudo leer.
    // Con HTTP hay tres casos distintos (2xx correcto, 502/503/504/429 transitorio, otro codigo definitivo); cada
    // condicion es ahora un metodo con nombre ({@code esCorrecto}, {@code esTransitorio} de MensajeHttp) y el
    // reintento solo ocurre en el caso transitorio.
    public Respuesta procesar(Solicitud solicitud) {
        if (solicitud == null) {
            return Respuesta.error("sin-id", EstadoRespuesta.SOLICITUD_INVALIDA, "La solicitud no puede ser nula");
        }
        String json = Marshalling.marshalSolicitud(solicitud);                      // marshalling
        URI destino = URI.create(urlBroker + RutasBroker.SOLICITAR);
        int intentos = reintentos + 1;
        IOException ultimoError = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                MensajeHttp mensaje = TransporteHttp.postJson(destino, json, null, msConexion, msLectura);
                if (mensaje.esCorrecto()) {
                    return Marshalling.unmarshalRespuesta(Marshalling.leerMensaje(mensaje.cuerpo())); // unmarshalling
                }
                if (!mensaje.esTransitorio()) {
                    return Respuesta.error(solicitud.getIdSolicitud(), EstadoRespuesta.ERROR_PROVEEDOR,
                            "El Broker respondio HTTP " + mensaje.codigo() + " en " + urlBroker);
                }
                ultimoError = new IOException("HTTP " + mensaje.codigo());
            } catch (IllegalArgumentException e) {
                return Respuesta.error(solicitud.getIdSolicitud(), EstadoRespuesta.ERROR_PROVEEDOR,
                        "El Broker devolvio una respuesta ilegible: " + e.getMessage());
            } catch (IOException e) {
                ultimoError = e;
            }
            Consola.cliente("Intento " + intento + "/" + intentos + " con el Broker " + urlBroker + " fallo: "
                    + (TransporteHttp.esTiempoAgotado(ultimoError) ? "tiempo agotado" : ultimoError.getMessage()));
            if (intento < intentos) {
                try {
                    Thread.sleep(300L * intento);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        boolean tiempo = ultimoError != null && TransporteHttp.esTiempoAgotado(ultimoError);
        return Respuesta.error(solicitud.getIdSolicitud(),
                tiempo ? EstadoRespuesta.TIEMPO_AGOTADO : EstadoRespuesta.BROKER_NO_DISPONIBLE,
                tiempo ? "El Broker no respondio a tiempo (" + urlBroker + ")"
                        : "No se pudo conectar con el Broker en " + urlBroker);
    }

    /**
     * Pide al Broker su estado (servicios registrados, métricas y últimas solicitudes).
     *
     * @throws IOException si el Broker no está disponible o responde algo ilegible
     */
    // REFACTOR: Change Function Declaration
    // RAZON: antes se consultaba el estado enviando un mensaje con tipo ESTADO por el socket ({@code
    // Canal.intercambiar}). Ahora es un {@code GET /api/broker/estado}: el nombre y el resultado (Map) se
    // conservan, por lo que AdminBrokerView y BrokerController no cambiaron.
    public Map<String, Object> consultarEstado() throws IOException {
        MensajeHttp mensaje = TransporteHttp.enviar("GET", URI.create(urlBroker + RutasBroker.ESTADO), null,
                Map.of("Accept", "application/json"), msConexion, msLectura);
        if (!mensaje.esCorrecto()) {
            throw new IOException("El Broker respondio HTTP " + mensaje.codigo());
        }
        try {
            return Marshalling.leerMensaje(mensaje.cuerpo());
        } catch (IllegalArgumentException e) {
            throw new IOException("Respuesta de estado ilegible: " + e.getMessage(), e);
        }
    }

    public URI getUrlBroker() {
        return urlBroker;
    }
}
