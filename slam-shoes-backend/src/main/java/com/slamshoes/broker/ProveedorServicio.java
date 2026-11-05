package com.slamshoes.broker;

import java.util.Set;

/**
 * Contrato común de todos los servicios que atiende el Broker.
 *
 * El Broker solo conoce esta interfaz, nunca las clases concretas. Reglas que
 * debe cumplir cada proveedor:
 * <ul>
 *   <li>Nunca devuelve null: siempre una {@link Respuesta} con el mismo idSolicitud recibido.</li>
 *   <li>Valida sus propios parámetros y, si son inválidos, responde con PARAMETROS_INVALIDOS.</li>
 *   <li>No atrapa las excepciones inesperadas: las captura el Broker (ERROR_PROVEEDOR).</li>
 *   <li>No devuelve entidades crudas ni datos sensibles (por ejemplo, nunca el hash de contraseña).</li>
 * </ul>
 */
public interface ProveedorServicio {

    /** Ejecuta la operación indicada en la solicitud y devuelve su resultado. */
    Respuesta ejecutar(Solicitud solicitud);

    /** Nombre con el que este proveedor se registra, por ejemplo "productos". */
    String getNombreServicio();

    /** Operaciones que ofrece; el Broker rechaza las que no estén en este conjunto. */
    Set<String> getOperacionesSoportadas();
}
