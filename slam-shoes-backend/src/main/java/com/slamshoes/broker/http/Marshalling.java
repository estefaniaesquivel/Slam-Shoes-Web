package com.slamshoes.broker.http;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * MARSHALLING y UNMARSHALLING del Broker.
 *
 * <ul>
 *   <li><b>marshal</b>: objeto Java ({@link Solicitud} / {@link Respuesta}) -> texto JSON que viaja
 *       en el CUERPO de una petición/respuesta HTTP.</li>
 *   <li><b>unmarshal</b>: texto JSON recibido -> objeto Java.</li>
 * </ul>
 *
 * Se hace en TRES lugares, siempre con esta clase: el cliente ({@link ClienteHttp}), el Broker
 * ({@link NucleoBroker}, {@link ProveedorHttp}) y el proveedor ({@link ExposicionProveedores}).
 * Ninguna otra clase sabe cómo luce el JSON.
 *
 * REFACTOR: Remove Dead Code (se quitó el campo "tipo" de los mensajes)
 * RAZÓN: con sockets crudos el campo "tipo" era el único modo de saber qué clase de mensaje llegaba por
 * la misma conexión. En HTTP lo dice la URL (/register, /solicitar...), así que el campo sobraba y
 * obligaba a esta clase a conocer el vocabulario del protocolo de sockets (Protocolo).
 */
public final class Marshalling {

    private Marshalling() {
    }

    // ------------------------------------------------------------------ solicitud

    /** Solicitud -> JSON. */
    public static String marshalSolicitud(Solicitud solicitud) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("idSolicitud", solicitud.getIdSolicitud());
        json.put("servicio", solicitud.getServicio());
        json.put("operacion", solicitud.getOperacion());
        json.put("parametros", solicitud.getParametros());
        json.put("fechaHora", solicitud.getFechaHora());
        return Json.escribir(json);
    }

    /**
     * JSON (ya convertido a mapa) -> Solicitud.
     *
     * @throws IllegalArgumentException si falta un campo obligatorio o tiene un tipo incorrecto
     */
    public static Solicitud unmarshalSolicitud(Map<String, Object> json) {
        String id = textoObligatorio(json, "idSolicitud");
        String servicio = textoObligatorio(json, "servicio");
        String operacion = textoObligatorio(json, "operacion");

        Object crudo = json.get("parametros");
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (crudo != null) {
            if (!(crudo instanceof Map<?, ?> mapa)) {
                throw new IllegalArgumentException("El campo 'parametros' debe ser un objeto JSON");
            }
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                parametros.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        return Solicitud.reconstruir(id, servicio, operacion, parametros, fecha(json.get("fechaHora")));
    }

    // ------------------------------------------------------------------ respuesta

    /** Respuesta -> JSON. */
    public static String marshalRespuesta(Respuesta respuesta) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("idSolicitud", respuesta.getIdSolicitud());
        json.put("estado", respuesta.getEstado());
        json.put("resultado", respuesta.getResultado());
        json.put("mensajeError", respuesta.getMensajeError());
        json.put("fechaHora", respuesta.getFechaHora());
        return Json.escribir(json);
    }

    /**
     * JSON (ya convertido a mapa) -> Respuesta.
     *
     * @throws IllegalArgumentException si el estado falta o no existe
     */
    public static Respuesta unmarshalRespuesta(Map<String, Object> json) {
        String id = textoObligatorio(json, "idSolicitud");
        String textoEstado = textoObligatorio(json, "estado");
        EstadoRespuesta estado;
        try {
            estado = EstadoRespuesta.valueOf(textoEstado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de respuesta desconocido: '" + textoEstado + "'");
        }
        Object mensaje = json.get("mensajeError");
        return Respuesta.reconstruir(id, json.get("resultado"), estado,
                mensaje == null ? null : mensaje.toString(), fecha(json.get("fechaHora")));
    }

    // ------------------------------------------------------------------ utilidades

    /** Lee el texto JSON recibido y lo convierte en mapa; lanza IllegalArgumentException si no es JSON válido. */
    public static Map<String, Object> leerMensaje(String linea) {
        try {
            return Json.leerObjeto(linea);
        } catch (Json.JsonException e) {
            throw new IllegalArgumentException("JSON mal formado: " + e.getMessage());
        }
    }

    private static String textoObligatorio(Map<String, Object> json, String campo) {
        Object valor = json.get(campo);
        if (!(valor instanceof String texto) || texto.isBlank()) {
            throw new IllegalArgumentException("Falta el campo '" + campo + "' (debe ser un texto)");
        }
        return texto;
    }

    private static LocalDateTime fecha(Object valor) {
        if (valor == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(valor.toString());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha invalida en 'fechaHora': '" + valor + "'");
        }
    }
}
