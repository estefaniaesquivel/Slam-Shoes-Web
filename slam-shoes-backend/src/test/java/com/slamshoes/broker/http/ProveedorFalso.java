package com.slamshoes.broker.http;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/** Proveedor de catálogo de mentira: no usa base de datos, solo cuenta cuántas veces se ejecutó. */
final class ProveedorFalso implements ProveedorServicio {

    final AtomicInteger ejecuciones = new AtomicInteger();
    private final String nombre;

    ProveedorFalso(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public Respuesta ejecutar(Solicitud s) {
        ejecuciones.incrementAndGet();
        switch (s.getOperacion()) {
            case "listarProductos":
                return Respuesta.exito(s.getIdSolicitud(), List.of(Map.of("idProducto", 1, "nombre", "Kyrie 5")));
            case "explotar":
                throw new IllegalStateException("fallo inesperado");
            case "fallarControlado":
                return Respuesta.error(s.getIdSolicitud(), EstadoRespuesta.PARAMETROS_INVALIDOS, "falta algo");
            default:
                return Respuesta.error(s.getIdSolicitud(), EstadoRespuesta.OPERACION_NO_SOPORTADA, "no existe");
        }
    }

    @Override
    public String getNombreServicio() {
        return nombre;
    }

    @Override
    public Set<String> getOperacionesSoportadas() {
        return Set.of("listarProductos", "explotar", "fallarControlado");
    }
}
