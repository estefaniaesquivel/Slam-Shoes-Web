package com.slamshoes.providers;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import static com.slamshoes.providers.ProveedorUtil.mapa;
import static com.slamshoes.providers.ProveedorUtil.obtenerLong;
import static com.slamshoes.providers.ProveedorUtil.obtenerTexto;

/**
 * Proveedor SOLO para pruebas del Nivel 2. Sirve para provocar, a voluntad, las situaciones
 * que hay que evidenciar: una excepción dentro del proveedor, un proveedor lento (timeout) y
 * una solicitud duplicada (el contador demuestra que la operación no se ejecutó dos veces).
 *
 * Operaciones: eco {texto}, fallar, dormir {ms}, contador.
 */
public class ServicioPruebas implements ProveedorServicio {

    private static final Set<String> OPERACIONES = Set.of("eco", "fallar", "dormir", "contador");
    private static final long MAXIMO_DORMIR_MS = 60_000;

    private final AtomicInteger ejecuciones = new AtomicInteger();

    @Override
    public String getNombreServicio() {
        return "pruebas";
    }

    @Override
    public Set<String> getOperacionesSoportadas() {
        return OPERACIONES;
    }

    @Override
    public Respuesta ejecutar(Solicitud solicitud) {
        String id = solicitud.getIdSolicitud();
        try {
            switch (solicitud.getOperacion()) {
                case "eco":
                    return Respuesta.exito(id, mapa("atendidoPor", getClass().getSimpleName(),
                            "eco", obtenerTexto(solicitud.getParametros(), "texto")));
                case "fallar":
                    // Excepción SIN atrapar: el proveedor no la maneja a propósito
                    throw new IllegalStateException("fallo simulado dentro del proveedor de pruebas");
                case "dormir":
                    long ms = obtenerLong(solicitud.getParametros(), "ms");
                    if (ms < 0 || ms > MAXIMO_DORMIR_MS) {
                        throw new IllegalArgumentException("'ms' debe estar entre 0 y " + MAXIMO_DORMIR_MS);
                    }
                    try {
                        Thread.sleep(ms);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("interrumpido mientras dormia");
                    }
                    return Respuesta.exito(id, mapa("atendidoPor", getClass().getSimpleName(), "durmioMs", ms));
                case "contador":
                    return Respuesta.exito(id, mapa("atendidoPor", getClass().getSimpleName(),
                            "ejecuciones", ejecuciones.incrementAndGet()));
                default:
                    return Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                            "Operacion no soportada: " + solicitud.getOperacion());
            }
        } catch (IllegalArgumentException e) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, e.getMessage());
        }
    }
}
