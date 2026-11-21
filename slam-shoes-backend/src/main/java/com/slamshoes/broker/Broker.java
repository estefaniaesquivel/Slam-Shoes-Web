package com.slamshoes.broker;

/**
 * Lo único que el cliente necesita saber del Broker: enviarle una solicitud y
 * recibir una respuesta.
 *
 * La implementa {@link BrokerCentral} (dentro del propio Broker) y
 * {@link com.slamshoes.broker.http.ClienteHttp} (desde otra aplicación, por HTTP/HTTPS),
 * sin cambiar el código del cliente.
 */
public interface Broker {

    /** Procesa la solicitud y devuelve siempre una respuesta, sin lanzar excepciones. */
    Respuesta procesar(Solicitud solicitud);
}
