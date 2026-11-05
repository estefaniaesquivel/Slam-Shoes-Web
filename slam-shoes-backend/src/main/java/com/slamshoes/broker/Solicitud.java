package com.slamshoes.broker;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mensaje que el cliente envía al Broker pidiendo una operación a un servicio.
 *
 * Es inmutable y solo contiene datos simples (textos, números, listas y mapas),
 * sin referencias a repositorios ni entidades JPA. Así, en el Nivel 2 podrá
 * convertirse a JSON sin cambios.
 */
public class Solicitud {

    private final String idSolicitud;
    private final String servicio;
    private final String operacion;
    private final Map<String, Object> parametros;
    private final LocalDateTime fechaHora;

    /**
     * Crea una solicitud nueva con identificador único y fecha actual.
     *
     * @param servicio   nombre del servicio registrado (productos, usuarios, pedidos)
     * @param operacion  nombre de la operación que ofrece ese servicio
     * @param parametros datos de la operación; puede ser null (se toma como vacío)
     * @throws IllegalArgumentException si el servicio o la operación están vacíos
     */
    public Solicitud(String servicio, String operacion, Map<String, Object> parametros) {
        if (servicio == null || servicio.isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio");
        }
        if (operacion == null || operacion.isBlank()) {
            throw new IllegalArgumentException("El nombre de la operacion es obligatorio");
        }
        this.idSolicitud = UUID.randomUUID().toString();
        this.servicio = servicio.trim();
        this.operacion = operacion.trim();
        this.parametros = (parametros == null)
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(parametros));
        this.fechaHora = LocalDateTime.now();
    }

    public String getIdSolicitud() { return idSolicitud; }

    /** Primeros 8 caracteres del UUID, suficientes para leer la consola. */
    public String getIdCorto() { return Consola.corto(idSolicitud); }

    public String getServicio() { return servicio; }

    public String getOperacion() { return operacion; }

    public Map<String, Object> getParametros() { return parametros; }

    public LocalDateTime getFechaHora() { return fechaHora; }

    /**
     * Copia de los parámetros apta para imprimir en consola: los valores de
     * cualquier parámetro cuyo nombre contenga "contrasena" se ocultan.
     */
    public Map<String, Object> getParametrosParaLog() {
        Map<String, Object> copia = new LinkedHashMap<>(parametros);
        for (Map.Entry<String, Object> entrada : copia.entrySet()) {
            if (entrada.getKey() != null && entrada.getKey().toLowerCase().contains("contrasena")) {
                entrada.setValue("****");
            }
        }
        return copia;
    }

    @Override
    public String toString() {
        return "Solicitud[" + getIdCorto() + " " + servicio + "." + operacion + " " + getParametrosParaLog() + "]";
    }
}
