package com.slamshoes.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import static com.slamshoes.providers.ProveedorUtil.mapa;
import static com.slamshoes.providers.ProveedorUtil.obtenerLong;

/**
 * Proveedor del servicio "productos": catálogo, detalle y existencias.
 *
 * NOTA (Nivel 1): por ahora trabaja con datos de ejemplo en memoria para poder
 * ejecutar la demo en una terminal sin Spring ni base de datos. En la versión
 * integrada se reemplaza la lista de ejemplo por ProductoRepository,
 * ProductoTallaRepository y CategoriaRepository; el contrato no cambia.
 */
public class ServicioProductos implements ProveedorServicio {

    private static final Set<String> OPERACIONES = Set.of("listarProductos", "obtenerDetalle", "consultarStock");

    private final List<Map<String, Object>> productos = new ArrayList<>();
    private final List<Map<String, Object>> variantes = new ArrayList<>();

    public ServicioProductos() {
        // Datos de ejemplo (equivalen a las tablas productos y producto_tallas)
        productos.add(mapa("idProducto", 1L, "nombre", "Kyrie 5 SpongeBob", "marca", "Nike",
                "categoria", "Basquetbol", "precio", 3499.0));
        productos.add(mapa("idProducto", 2L, "nombre", "Kyrie Infinity Keep Sue Fresh", "marca", "Nike",
                "categoria", "Basquetbol", "precio", 3199.0));
        productos.add(mapa("idProducto", 3L, "nombre", "Air Jordan 1 Mid", "marca", "Jordan",
                "categoria", "Casual", "precio", 2899.0));

        variantes.add(mapa("idVariante", 101L, "idProducto", 1L, "talla", "26.0", "color", "Amarillo", "stock", 5));
        variantes.add(mapa("idVariante", 102L, "idProducto", 1L, "talla", "27.0", "color", "Amarillo", "stock", 0));
        variantes.add(mapa("idVariante", 103L, "idProducto", 1L, "talla", "28.0", "color", "Amarillo", "stock", 3));
        variantes.add(mapa("idVariante", 201L, "idProducto", 2L, "talla", "26.5", "color", "Verde", "stock", 4));
        variantes.add(mapa("idVariante", 202L, "idProducto", 2L, "talla", "27.5", "color", "Verde", "stock", 2));
        variantes.add(mapa("idVariante", 301L, "idProducto", 3L, "talla", "27.0", "color", "Negro", "stock", 6));
    }

    @Override
    public String getNombreServicio() {
        return "productos";
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
                case "listarProductos" -> Respuesta.exito(id, listarProductos());
                case "obtenerDetalle" -> Respuesta.exito(id, obtenerDetalle(solicitud.getParametros()));
                case "consultarStock" -> Respuesta.exito(id, consultarStock(solicitud.getParametros()));
                default -> Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                        "Operacion no soportada: " + solicitud.getOperacion());
            };
        } catch (IllegalArgumentException e) {
            // Parametros faltantes, mal formados o que apuntan a algo inexistente
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, e.getMessage());
        }
    }

    private Map<String, Object> listarProductos() {
        List<Map<String, Object>> resumen = new ArrayList<>();
        for (Map<String, Object> p : productos) {
            resumen.add(mapa("idProducto", p.get("idProducto"), "nombre", p.get("nombre"),
                    "marca", p.get("marca"), "precio", p.get("precio")));
        }
        return mapa("atendidoPor", getClass().getSimpleName(), "totalProductos", resumen.size(), "productos", resumen);
    }

    private Map<String, Object> obtenerDetalle(Map<String, Object> parametros) {
        long idProducto = obtenerLong(parametros, "idProducto");
        Map<String, Object> producto = productos.stream()
                .filter(p -> p.get("idProducto").equals(idProducto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe un producto con idProducto=" + idProducto));

        List<Map<String, Object>> tallas = new ArrayList<>();
        for (Map<String, Object> v : variantes) {
            if (v.get("idProducto").equals(idProducto)) {
                tallas.add(mapa("idVariante", v.get("idVariante"), "talla", v.get("talla"),
                        "color", v.get("color"), "stock", v.get("stock")));
            }
        }
        return mapa("atendidoPor", getClass().getSimpleName(),
                "idProducto", producto.get("idProducto"), "nombre", producto.get("nombre"),
                "marca", producto.get("marca"), "categoria", producto.get("categoria"),
                "precio", producto.get("precio"), "tallas", tallas);
    }

    private Map<String, Object> consultarStock(Map<String, Object> parametros) {
        long idVariante = obtenerLong(parametros, "idVariante");
        Map<String, Object> variante = variantes.stream()
                .filter(v -> v.get("idVariante").equals(idVariante))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe una variante con idVariante=" + idVariante));
        int stock = (Integer) variante.get("stock");
        return mapa("atendidoPor", getClass().getSimpleName(), "idVariante", idVariante,
                "talla", variante.get("talla"), "stock", stock, "disponible", stock > 0);
    }
}
