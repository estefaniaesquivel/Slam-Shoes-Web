package com.slamshoes.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.slamshoes.broker.Broker;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.broker.red.ClienteSocket;

/**
 * UNICA puerta de la aplicacion web hacia el Broker. Los controladores ya no conocen repositorios
 * ni logica de negocio: arman una solicitud, se la dan a este gateway y traducen la respuesta a HTTP.
 *
 * Solo depende de la interfaz {@link Broker}: no importa ninguna clase concreta de proveedores.
 */
@Service
public class BrokerGateway {

    private final ClienteSocket cliente;

    public BrokerGateway(@Value("${broker.host:localhost}") String host,
                         @Value("${broker.puerto:9000}") int puerto,
                         @Value("${broker.timeout-ms:20000}") int timeoutMs) {
        this.cliente = new ClienteSocket(host, puerto, 2000, timeoutMs, 1);
    }

    /** Envia una solicitud nueva (con identificador propio) y devuelve siempre una respuesta. */
    public Respuesta enviar(String servicio, String operacion, Map<String, Object> parametros) {
        return enviar(new Solicitud(servicio, operacion, parametros));
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
