package com.slamshoes.broker.cliente;

import java.util.Map;

import com.slamshoes.broker.Broker;
import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * Cliente del Broker: construye la solicitud, la envía y muestra la respuesta.
 *
 * Solo depende de la interfaz {@link Broker}. No importa ninguna clase de
 * com.slamshoes.providers, así que no puede instanciar proveedores ni saber
 * quién atiende cada servicio.
 */
public class ClienteBroker {

    private final Broker broker;

    public ClienteBroker(Broker broker) {
        this.broker = broker;
    }

    /** Arma una Solicitud, la envía al Broker, imprime la respuesta y la devuelve. */
    public Respuesta enviar(String servicio, String operacion, Map<String, Object> parametros) {
        Solicitud solicitud;
        try {
            solicitud = new Solicitud(servicio, operacion, parametros);
        } catch (IllegalArgumentException e) {
            Consola.cliente("No se pudo construir la solicitud: " + e.getMessage());
            return Respuesta.error("sin-id", EstadoRespuesta.SOLICITUD_INVALIDA, e.getMessage());
        }

        Consola.cliente("Enviando " + solicitud.getIdCorto() + " " + servicio + "." + operacion
                + " " + solicitud.getParametrosParaLog());
        Respuesta respuesta = broker.procesar(solicitud);
        mostrar(respuesta);
        return respuesta;
    }

    /** Presenta la respuesta en consola: estado y resultado, o el mensaje de error. */
    public void mostrar(Respuesta respuesta) {
        Consola.cliente("Recibido " + respuesta.getIdCorto() + " estado=" + respuesta.getEstado());
        if (respuesta.isExitosa()) {
            for (String linea : Consola.aLineas(respuesta.getResultado())) {
                Consola.cliente("  " + linea);
            }
        } else {
            Consola.cliente("  error: " + respuesta.getMensajeError());
        }
    }
}
