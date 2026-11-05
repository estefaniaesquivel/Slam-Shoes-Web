package com.slamshoes.providers;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import static com.slamshoes.providers.ProveedorUtil.mapa;
import static com.slamshoes.providers.ProveedorUtil.obtenerLong;
import static com.slamshoes.providers.ProveedorUtil.obtenerTexto;
import static com.slamshoes.providers.ProveedorUtil.resumirItems;

/**
 * Proveedor alterno del servicio "pedidos", usado para demostrar la sustitución.
 *
 * Cumple el mismo contrato que {@link ServicioPedidos}, pero no guarda nada:
 * el historial siempre sale vacío y los pedidos nuevos quedan en estado SIMULADO.
 * También sirve para correr pruebas sin conexión a la base de datos.
 */
public class ServicioPedidosEnMemoria implements ProveedorServicio {

    private static final Set<String> OPERACIONES = Set.of("crearPedido", "consultarHistorial");

    private final AtomicLong siguienteId = new AtomicLong(1);

    @Override
    public String getNombreServicio() {
        return "pedidos";
    }

    @Override
    public Set<String> getOperacionesSoportadas() {
        return OPERACIONES;
    }

    @Override
    public Respuesta ejecutar(Solicitud solicitud) {
        String id = solicitud.getIdSolicitud();
        try {
            return switch (solicitud.getOperacion()) {
                case "consultarHistorial" -> {
                    long idUsuario = obtenerLong(solicitud.getParametros(), "idUsuario");
                    yield Respuesta.exito(id, mapa("atendidoPor", getClass().getSimpleName(),
                            "idUsuario", idUsuario, "totalPedidos", 0, "pedidos", List.of(),
                            "nota", "Proveedor alterno: no guarda pedidos"));
                }
                case "crearPedido" -> {
                    long idUsuario = obtenerLong(solicitud.getParametros(), "idUsuario");
                    String direccion = obtenerTexto(solicitud.getParametros(), "direccionEnvio");
                    int[] resumen = resumirItems(solicitud.getParametros());
                    yield Respuesta.exito(id, mapa("atendidoPor", getClass().getSimpleName(),
                            "idPedido", siguienteId.getAndIncrement(), "idUsuario", idUsuario,
                            "estado", "SIMULADO", "lineas", resumen[0], "unidades", resumen[1],
                            "direccionEnvio", direccion));
                }
                default -> Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                        "Operacion no soportada: " + solicitud.getOperacion());
            };
        } catch (IllegalArgumentException e) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, e.getMessage());
        }
    }
}
