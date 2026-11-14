package com.slamshoes.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;

/**
 * Traduce una {@link Respuesta} del Broker a HTTP. Es lo unico que un controlador debe hacer
 * con ella: el estado decide el codigo HTTP y el mensaje viaja en el campo "error", que es el que
 * ya lee el frontend.
 */
final class RespuestaHttp {

    private RespuestaHttp() {
    }

    static int codigo(EstadoRespuesta estado) {
        switch (estado) {
            case EXITO:
                return 200;
            case SOLICITUD_INVALIDA:
            case PARAMETROS_INVALIDOS:
            case OPERACION_NO_SOPORTADA:
                return 400;
            case NO_AUTORIZADO:
                return 401;
            case SERVICIO_NO_ENCONTRADO:
                return 404;
            case CONFLICTO:
                return 409;
            case PROVEEDOR_NO_DISPONIBLE:
            case BROKER_NO_DISPONIBLE:
                return 503;
            case TIEMPO_AGOTADO:
                return 504;
            case ERROR_PROVEEDOR:
            default:
                return 500;
        }
    }

    /** Cuerpo de error: {error, estado, idSolicitud}. */
    static ResponseEntity<Map<String, Object>> error(Respuesta r) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", r.getMensajeError() == null ? "No se pudo completar la operacion" : r.getMensajeError());
        cuerpo.put("estado", r.getEstado().name());
        cuerpo.put("idSolicitud", r.getIdSolicitud());
        return ResponseEntity.status(codigo(r.getEstado())).body(cuerpo);
    }

    /** Resultado del proveedor como mapa (o vacio si no era un objeto). */
    @SuppressWarnings("unchecked")
    static Map<String, Object> resultadoComoMapa(Respuesta r) {
        return r.getResultado() instanceof Map ? (Map<String, Object>) r.getResultado() : new LinkedHashMap<>();
    }
}
