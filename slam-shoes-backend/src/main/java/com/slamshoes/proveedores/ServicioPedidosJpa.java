package com.slamshoes.proveedores;

import static com.slamshoes.providers.ProveedorUtil.convertirALong;
import static com.slamshoes.providers.ProveedorUtil.mapa;
import static com.slamshoes.providers.ProveedorUtil.obtenerLong;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.model.Pedido;
import com.slamshoes.model.PedidoDetalle;
import com.slamshoes.model.Producto;
import com.slamshoes.model.ProductoTalla;
import com.slamshoes.repository.CarritoDetalleRepository;
import com.slamshoes.repository.CarritoRepository;
import com.slamshoes.repository.PedidoDetalleRepository;
import com.slamshoes.repository.PedidoRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.ProductoTallaRepository;
import com.slamshoes.repository.UsuarioRepository;
import com.slamshoes.service.OfertaService;

/**
 * Proveedor REAL del servicio "pedidos" (con base de datos). Aqui vive la secuencia de reglas de
 * negocio que antes estaba pegada a PedidoController: validar usuario, validar stock, calcular
 * precios con la oferta vigente, guardar pedido y detalle, descontar existencias y vaciar el carrito.
 * Todo en UNA transaccion: si algo falla, no se guarda nada a medias.
 *
 * El pago es simulado (fuera del alcance del proyecto); cuando exista, se agregara un proveedor
 * "pagos" y este solo tendra que solicitarlo a traves del Broker.
 *
 * Operaciones:
 *  - crearPedido        {idUsuario, direccionEnvio, metodoPago?, items:[{idVariante, cantidad}]}
 *  - consultarHistorial {idUsuario}
 */
@Component
public class ServicioPedidosJpa implements ProveedorServicio {

    private static final String NOMBRE_CLASE = "ServicioPedidosJpa";
    private static final Set<String> OPERACIONES = Set.of("crearPedido", "consultarHistorial");

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PedidoDetalleRepository pedidoDetalleRepository;
    @Autowired private ProductoTallaRepository productoTallaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CarritoRepository carritoRepository;
    @Autowired private CarritoDetalleRepository carritoDetalleRepository;
    @Autowired private OfertaService ofertaService;

    @Override
    public String getNombreServicio() {
        return "pedidos";
    }

    @Override
    public Set<String> getOperacionesSoportadas() {
        return OPERACIONES;
    }

    /**
     * Toda la ejecucion corre en una transaccion. Los errores de validacion se devuelven como
     * Respuesta (no se lanzan), por lo que la transaccion termina sin haber guardado nada; una
     * excepcion inesperada SI la revierte y llega al Broker como ERROR_PROVEEDOR.
     */
    @Override
    @Transactional
    public Respuesta ejecutar(Solicitud solicitud) {
        String id = solicitud.getIdSolicitud();
        try {
            switch (solicitud.getOperacion()) {
                case "crearPedido":
                    return crearPedido(id, solicitud.getParametros());
                case "consultarHistorial":
                    return Respuesta.exito(id, consultarHistorial(solicitud.getParametros()));
                default:
                    return Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                            "Operacion no soportada: " + solicitud.getOperacion());
            }
        } catch (IllegalArgumentException e) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ crearPedido

    private Respuesta crearPedido(String id, Map<String, Object> parametros) {
        long idUsuario = obtenerLong(parametros, "idUsuario");
        if (!usuarioRepository.existsById(idUsuario)) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, "Usuario no válido");
        }
        Object direccionCruda = parametros.get("direccionEnvio");
        if (direccionCruda == null || direccionCruda.toString().isBlank()) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, "Falta la dirección de envío");
        }
        String direccion = direccionCruda.toString().trim();
        Object metodoCrudo = parametros.get("metodoPago");
        String metodoPago = metodoCrudo == null || metodoCrudo.toString().isBlank() ? null : metodoCrudo.toString().trim();

        // Si la misma talla viene en dos lineas se suman, para validar el stock sobre el total
        Map<Long, Integer> cantidades = leerItems(parametros);

        // 1) Validar todo antes de guardar nada
        List<ProductoTalla> variantes = new ArrayList<>();
        List<Double> precios = new ArrayList<>();
        List<Integer> unidades = new ArrayList<>();
        double total = 0;

        for (Map.Entry<Long, Integer> linea : cantidades.entrySet()) {
            ProductoTalla variante = productoTallaRepository.findById(linea.getKey()).orElse(null);
            if (variante == null) {
                return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, "Una de las tallas seleccionadas ya no existe");
            }
            Producto producto = productoRepository.findById(variante.getIdProducto()).orElse(null);
            if (producto == null || Boolean.FALSE.equals(producto.getActivo())) {
                return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, "Un producto del carrito ya no está disponible");
            }
            int stock = variante.getStock() == null ? 0 : variante.getStock();
            if (stock < linea.getValue()) {
                return Respuesta.error(id, EstadoRespuesta.CONFLICTO,
                        "Sin stock suficiente de \"" + producto.getNombre() + "\" talla " + variante.getTalla()
                                + " (disponibles: " + stock + ")");
            }
            double precio = ofertaService.precioFinal(
                    producto.getPrecio() == null ? 0 : producto.getPrecio(),
                    ofertaService.mejorDeProducto(producto.getIdProducto()).orElse(null));

            variantes.add(variante);
            precios.add(precio);
            unidades.add(linea.getValue());
            total += precio * linea.getValue();
        }

        // 2) Guardar pedido y detalle, descontar existencias
        Pedido pedido = new Pedido();
        pedido.setIdUsuario(idUsuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setTotal(Math.round(total * 100.0) / 100.0);
        pedido.setEstado("PENDIENTE");
        pedido.setDireccionEnvio(direccion);
        pedido.setMetodoPago(metodoPago);
        pedido = pedidoRepository.save(pedido);

        int totalUnidades = 0;
        for (int i = 0; i < variantes.size(); i++) {
            ProductoTalla variante = variantes.get(i);

            PedidoDetalle detalle = new PedidoDetalle();
            detalle.setIdPedido(pedido.getIdPedido());
            detalle.setIdVariante(variante.getIdVariante());
            detalle.setCantidad(unidades.get(i));
            detalle.setPrecioUnitario(precios.get(i));
            pedidoDetalleRepository.save(detalle);

            variante.setStock(variante.getStock() - unidades.get(i));
            productoTallaRepository.save(variante);
            totalUnidades += unidades.get(i);
        }

        // 3) Vaciar el carrito guardado en la BD
        carritoRepository.findByIdUsuario(idUsuario)
                .ifPresent(c -> carritoDetalleRepository.deleteByIdCarrito(c.getIdCarrito()));

        return Respuesta.exito(id, mapa("atendidoPor", NOMBRE_CLASE, "idPedido", pedido.getIdPedido(),
                "idUsuario", idUsuario, "total", pedido.getTotal(), "estado", pedido.getEstado(),
                "lineas", variantes.size(), "unidades", totalUnidades, "direccionEnvio", direccion));
    }

    /** Valida "items" y agrupa por talla. Lanza IllegalArgumentException si algo no es valido. */
    private Map<Long, Integer> leerItems(Map<String, Object> parametros) {
        Object crudo = parametros.get("items");
        if (!(crudo instanceof List<?> items) || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido no tiene productos");
        }
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> linea)) {
                throw new IllegalArgumentException("Producto o cantidad no válidos");
            }
            long idVariante = convertirALong(linea.get("idVariante"), "items.idVariante");
            long cantidad = convertirALong(linea.get("cantidad"), "items.cantidad");
            if (cantidad <= 0 || cantidad > 1000) {
                throw new IllegalArgumentException("Producto o cantidad no válidos (cantidad recibida: " + cantidad + ")");
            }
            cantidades.merge(idVariante, (int) cantidad, Integer::sum);
        }
        return cantidades;
    }

    // ------------------------------------------------------------------ consultarHistorial

    private Map<String, Object> consultarHistorial(Map<String, Object> parametros) {
        long idUsuario = obtenerLong(parametros, "idUsuario");
        List<Pedido> pedidos = pedidoRepository.findByIdUsuario(idUsuario, Sort.by(Sort.Direction.DESC, "fecha"));

        // Detalles de todos los pedidos y luego tallas y productos en bloque (no una consulta por linea)
        Map<Long, List<PedidoDetalle>> detallesPorPedido = new HashMap<>();
        List<Long> idsVariante = new ArrayList<>();
        for (Pedido p : pedidos) {
            List<PedidoDetalle> detalles = pedidoDetalleRepository.findByIdPedido(p.getIdPedido());
            detallesPorPedido.put(p.getIdPedido(), detalles);
            detalles.forEach(d -> idsVariante.add(d.getIdVariante()));
        }
        Map<Long, ProductoTalla> variantes = productoTallaRepository.findAllById(idsVariante).stream()
                .collect(Collectors.toMap(ProductoTalla::getIdVariante, v -> v, (a, b) -> a));
        List<Long> idsProducto = variantes.values().stream().map(ProductoTalla::getIdProducto).distinct().toList();
        Map<Long, Producto> productos = productoRepository.findAllById(idsProducto).stream()
                .collect(Collectors.toMap(Producto::getIdProducto, p -> p, (a, b) -> a));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Pedido p : pedidos) {
            List<Map<String, Object>> lineas = new ArrayList<>();
            for (PedidoDetalle d : detallesPorPedido.get(p.getIdPedido())) {
                ProductoTalla v = variantes.get(d.getIdVariante());
                Producto prod = v == null ? null : productos.get(v.getIdProducto());
                lineas.add(mapa("idProducto", prod == null ? null : prod.getIdProducto(),
                        "nombre", prod == null ? "Producto no disponible" : prod.getNombre(),
                        "imagenUrl", prod == null ? null : prod.getImagenUrl(),
                        "talla", v == null ? null : v.getTalla(),
                        "color", v == null ? null : v.getColor(),
                        "cantidad", d.getCantidad(), "precioUnitario", d.getPrecioUnitario()));
            }
            resultado.add(mapa("idPedido", p.getIdPedido(), "fecha", p.getFecha(), "total", p.getTotal(),
                    "estado", p.getEstado(), "direccionEnvio", p.getDireccionEnvio(),
                    "metodoPago", p.getMetodoPago(), "lineas", lineas));
        }
        return mapa("atendidoPor", NOMBRE_CLASE, "idUsuario", idUsuario, "totalPedidos", resultado.size(),
                "pedidos", resultado);
    }
}
