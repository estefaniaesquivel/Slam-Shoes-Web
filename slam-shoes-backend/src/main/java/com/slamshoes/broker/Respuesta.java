package com.slamshoes.broker;

import java.time.LocalDateTime;

/**
 * Resultado que el Broker devuelve al cliente.
 *
 * Es inmutable. Se construye siempre con los métodos de fábrica
 * {@link #exito(String, Object)} y {@link #error(String, EstadoRespuesta, String)}
 * para que ningún proveedor arme respuestas a medias.
 */
public class Respuesta {

    private final String idSolicitud;
    private final Object resultado;
    private final EstadoRespuesta estado;
    private final String mensajeError;
    private final LocalDateTime fechaHora;

    private Respuesta(String idSolicitud, Object resultado, EstadoRespuesta estado, String mensajeError) {
        this.idSolicitud = idSolicitud;
        this.resultado = resultado;
        this.estado = estado;
        this.mensajeError = mensajeError;
        this.fechaHora = LocalDateTime.now();
    }

    /** Respuesta correcta con los datos de la operación. */
    public static Respuesta exito(String idSolicitud, Object resultado) {
        return new Respuesta(idSolicitud, resultado, EstadoRespuesta.EXITO, null);
    }

    /** Respuesta de error: el resultado es null y se incluye un mensaje legible. */
    public static Respuesta error(String idSolicitud, EstadoRespuesta estado, String mensajeError) {
        if (estado == EstadoRespuesta.EXITO) {
            throw new IllegalArgumentException("Una respuesta de error no puede tener estado EXITO");
        }
        return new Respuesta(idSolicitud, null, estado, mensajeError);
    }

    public boolean isExitosa() { return estado == EstadoRespuesta.EXITO; }

    public String getIdSolicitud() { return idSolicitud; }

    public String getIdCorto() { return Consola.corto(idSolicitud); }

    public Object getResultado() { return resultado; }

    public EstadoRespuesta getEstado() { return estado; }

    public String getMensajeError() { return mensajeError; }

    public LocalDateTime getFechaHora() { return fechaHora; }

    @Override
    public String toString() {
        return "Respuesta[" + getIdCorto() + " estado=" + estado
                + (mensajeError != null ? " error=" + mensajeError : "") + "]";
    }
}
