package com.slamshoes.broker;

/**
 * Lo único que el cliente necesita saber del Broker: enviarle una solicitud y
 * recibir una respuesta.
 *
 * En el Nivel 1 la implementa {@link BrokerCentral} (misma aplicación). En el
 * Nivel 2 se podrá agregar otra implementación que hable por sockets, sin
 * cambiar el código del cliente.
 */
public interface Broker {

    /** Procesa la solicitud y devuelve siempre una respuesta, sin lanzar excepciones. */
    Respuesta procesar(Solicitud solicitud);
}
