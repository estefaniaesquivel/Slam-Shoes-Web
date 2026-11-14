package com.slamshoes.broker.red;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

import com.slamshoes.broker.Broker;
import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * Implementación de {@link Broker} que habla con un Broker REMOTO por un socket TCP.
 *
 * Es la "otra implementación" que el Nivel 1 dejó prevista: {@link com.slamshoes.broker.cliente.ClienteBroker}
 * solo conoce la interfaz Broker, así que pasa del Broker local al distribuido SIN cambiar una línea.
 *
 * Nunca lanza excepciones: si el Broker está apagado o no contesta a tiempo devuelve una Respuesta
 * con estado BROKER_NO_DISPONIBLE o TIEMPO_AGOTADO. Los reintentos (limitados) usan el MISMO
 * idSolicitud, y el Broker descarta los duplicados.
 */
public class ClienteSocket implements Broker {

    private final String host;
    private final int puerto;
    private final int msConexion;
    private final int msLectura;
    private final int reintentos;

    public ClienteSocket(String host, int puerto) {
        this(host, puerto, 2000, 20_000, 1);
    }

    public ClienteSocket(String host, int puerto, int msConexion, int msLectura, int reintentos) {
        this.host = host;
        this.puerto = puerto;
        this.msConexion = msConexion;
        this.msLectura = msLectura;
        this.reintentos = Math.max(0, reintentos);
    }

    @Override
    public Respuesta procesar(Solicitud solicitud) {
        if (solicitud == null) {
            return Respuesta.error("sin-id", EstadoRespuesta.SOLICITUD_INVALIDA, "La solicitud no puede ser nula");
        }
        String json = Marshalling.marshalSolicitud(solicitud);                      // marshalling
        int intentos = reintentos + 1;
        IOException ultimoError = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                String linea = Canal.intercambiar(host, puerto, json, msConexion, msLectura);
                return Marshalling.unmarshalRespuesta(Marshalling.leerMensaje(linea));  // unmarshalling
            } catch (IllegalArgumentException e) {
                return Respuesta.error(solicitud.getIdSolicitud(), EstadoRespuesta.ERROR_PROVEEDOR,
                        "El Broker devolvio una respuesta ilegible: " + e.getMessage());
            } catch (IOException e) {
                ultimoError = e;
                Consola.cliente("Intento " + intento + "/" + intentos + " con el Broker " + host + ":" + puerto
                        + " fallo: " + (e instanceof SocketTimeoutException ? "tiempo agotado" : e.getMessage()));
                if (intento < intentos) {
                    try {
                        Thread.sleep(300L * intento);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        boolean tiempo = ultimoError instanceof SocketTimeoutException;
        return Respuesta.error(solicitud.getIdSolicitud(),
                tiempo ? EstadoRespuesta.TIEMPO_AGOTADO : EstadoRespuesta.BROKER_NO_DISPONIBLE,
                tiempo ? "El Broker no respondio a tiempo (" + host + ":" + puerto + ")"
                        : "No se pudo conectar con el Broker en " + host + ":" + puerto);
    }

    /**
     * Pide al Broker su estado (servicios registrados, métricas y últimas solicitudes).
     *
     * @throws IOException si el Broker no está disponible
     */
    public Map<String, Object> consultarEstado() throws IOException {
        String linea = Canal.intercambiar(host, puerto,
                Json.escribir(Map.of(Protocolo.TIPO, Protocolo.ESTADO)), msConexion, msLectura);
        try {
            return Marshalling.leerMensaje(linea);
        } catch (IllegalArgumentException e) {
            throw new IOException("Respuesta de estado ilegible: " + e.getMessage(), e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPuerto() {
        return puerto;
    }
}
