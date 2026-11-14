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
    ERROR_PROVEEDOR,

    // ---- Estados agregados en el Nivel 2 (Broker distribuido) ----

    /** Credenciales incorrectas (HTTP 401). */
    NO_AUTORIZADO,

    /** La operación choca con el estado actual: correo ya registrado, sin stock... (HTTP 409). */
    CONFLICTO,

    /** El proveedor está registrado pero no se pudo contactar (caído o rechazó la conexión). */
    PROVEEDOR_NO_DISPONIBLE,

    /** El proveedor o el Broker no respondieron dentro del tiempo límite. */
    TIEMPO_AGOTADO,

    /** El cliente no pudo comunicarse con el Broker (apagado o inalcanzable). */
    BROKER_NO_DISPONIBLE
}
