package com.slamshoes.providers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * Proveedor del servicio "pedidos": crear pedidos y consultar el historial.
 *
 * NOTA (Nivel 1): guarda los pedidos en memoria, con dos pedidos de ejemplo para
 * el usuario 1. En la versión integrada usará PedidoRepository y
 * PedidoDetalleRepository, calculará el total con los precios de la base de datos
 * y validará y descontará el stock de producto_tallas.
 */
public class ServicioPedidos implements ProveedorServicio {

    private static final Set<String> OPERACIONES = Set.of("crearPedido", "consultarHistorial");

    private final Map<Long, List<Map<String, Object>>> pedidosPorUsuario = new ConcurrentHashMap<>();
    private final AtomicLong siguienteIdPedido = new AtomicLong(1003);

    public ServicioPedidos() {
        List<Map<String, Object>> deUsuarioUno = new CopyOnWriteArrayList<>();
        deUsuarioUno.add(mapa("idPedido", 1001L, "fecha", "2026-10-28T18:30:00", "estado", "ENTREGADO", "unidades", 2));
        deUsuarioUno.add(mapa("idPedido", 1002L, "fecha", "2026-11-02T12:05:00", "estado", "EN_CAMINO", "unidades", 1));
        pedidosPorUsuario.put(1L, deUsuarioUno);
    }

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
                case "consultarHistorial" -> Respuesta.exito(id, consultarHistorial(solicitud.getParametros()));
                case "crearPedido" -> Respuesta.exito(id, crearPedido(solicitud.getParametros()));
                default -> Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                        "Operacion no soportada: " + solicitud.getOperacion());
            };
        } catch (IllegalArgumentException e) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, e.getMessage());
        }
    }

    private Map<String, Object> consultarHistorial(Map<String, Object> parametros) {
        long idUsuario = obtenerLong(parametros, "idUsuario");
        List<Map<String, Object>> pedidos = new ArrayList<>(pedidosPorUsuario.getOrDefault(idUsuario, List.of()));
        return mapa("atendidoPor", getClass().getSimpleName(), "idUsuario", idUsuario,
                "totalPedidos", pedidos.size(), "pedidos", pedidos);
    }

    private Map<String, Object> crearPedido(Map<String, Object> parametros) {
        long idUsuario = obtenerLong(parametros, "idUsuario");
        String direccionEnvio = obtenerTexto(parametros, "direccionEnvio");
        int[] resumen = resumirItems(parametros); // {lineas, unidades}; lanza IllegalArgumentException si algo falla

        long idPedido = siguienteIdPedido.getAndIncrement();
        Map<String, Object> pedido = mapa("idPedido", idPedido,
                "fecha", LocalDateTime.now().withNano(0).toString(),
                "estado", "PENDIENTE", "unidades", resumen[1]);
        pedidosPorUsuario.computeIfAbsent(idUsuario, k -> new CopyOnWriteArrayList<>()).add(pedido);

        return mapa("atendidoPor", getClass().getSimpleName(), "idPedido", idPedido, "idUsuario", idUsuario,
                "estado", "PENDIENTE", "lineas", resumen[0], "unidades", resumen[1],
                "direccionEnvio", direccionEnvio);
    }
}
