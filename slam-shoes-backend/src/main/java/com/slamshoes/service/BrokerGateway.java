package com.slamshoes.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.slamshoes.broker.Broker;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.broker.http.ClienteHttp;
import com.slamshoes.exception.StockInsuficienteException;

/**
 * UNICA puerta de la aplicacion web hacia el Broker. Los controladores ya no conocen repositorios
 * ni logica de negocio: arman una solicitud, se la dan a este gateway y traducen la respuesta a HTTP.
 *
 * Solo depende de la interfaz {@link Broker}: no importa ninguna clase concreta de proveedores.
 * Habla con el Broker por HTTP/HTTPS usando la URL de la variable de entorno {@code BROKER_URL}.
 *
 * REFACTOR: Change Function Declaration
 * RAZON: el constructor pedia host, puerto TCP y timeout, tres datos que en Render no existen (solo hay una URL
 * HTTPS). Ahora recibe la URL del Broker y el timeout; el cuerpo del servicio no cambia.
 */
@Service
public class BrokerGateway {

    private final ClienteHttp cliente;

    public BrokerGateway(@Value("${broker.url:http://localhost:${server.port:8080}}") String urlBroker,
                         @Value("${broker.timeout-ms:20000}") int timeoutMs) {
        this.cliente = new ClienteHttp(urlBroker, 2000, timeoutMs, 1);
    }

    /** Envia una solicitud nueva (con identificador propio) y devuelve siempre una respuesta. */
    public Respuesta enviar(String servicio, String operacion, Map<String, Object> parametros) {
        return enviar(new Solicitud(servicio, operacion, parametros));
    }

    /**
     * Crea un pedido y restaura el tipo de error de stock que viaja como conflicto por el Broker.
     */
    public Respuesta crearPedido(Map<String, Object> parametros) {
        Respuesta respuesta = enviar("pedidos", "crearPedido", parametros);
        if (respuesta.getEstado() == EstadoRespuesta.CONFLICTO) {
            String mensaje = respuesta.getMensajeError() == null
                    ? "No se pudo completar el pedido por falta de stock"
                    : respuesta.getMensajeError();
            throw new StockInsuficienteException(mensaje);
        }
        return respuesta;
    }

    /** Envia una solicitud ya construida (por ejemplo, con el idSolicitud que eligio el navegador). */
    public Respuesta enviar(Solicitud solicitud) {
        Broker broker = cliente;
        return broker.procesar(solicitud);
    }

    /** Estado del Broker (servicios registrados, metricas, ultimas solicitudes) para el monitoreo. */
    public Map<String, Object> estado() throws IOException {
        return cliente.consultarEstado();
    }
}
