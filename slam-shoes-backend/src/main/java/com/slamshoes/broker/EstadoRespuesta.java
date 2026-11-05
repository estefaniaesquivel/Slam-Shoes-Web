package com.slamshoes.broker;

/**
 * Estados posibles de una {@link Respuesta} del Broker.
 *
 * El cliente solo necesita mirar este valor para saber si la operación salió bien
 * o qué tipo de problema ocurrió, sin interpretar mensajes de texto.
 */
public enum EstadoRespuesta {

    /** La operación se ejecutó correctamente. */
    EXITO,

    /** La solicitud llegó nula o mal formada al Broker. */
    SOLICITUD_INVALIDA,

    /** Nadie registró un proveedor con ese nombre de servicio. */
    SERVICIO_NO_ENCONTRADO,

    /** El proveedor existe, pero no ofrece esa operación. */
    OPERACION_NO_SOPORTADA,

    /** Faltan parámetros, o tienen un formato o valor inválido. */
    PARAMETROS_INVALIDOS,

    /** El proveedor lanzó una excepción o devolvió una respuesta inválida. */
    ERROR_PROVEEDOR
}
