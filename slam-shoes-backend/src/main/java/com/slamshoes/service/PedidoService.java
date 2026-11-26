package com.slamshoes.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slamshoes.dto.ItemPedidoDTO;
import com.slamshoes.dto.LineaPedidoDTO;
import com.slamshoes.dto.PedidoClienteDTO;
import com.slamshoes.dto.PedidoFilaDTO;
import com.slamshoes.dto.PedidoRequestDTO;
import com.slamshoes.exception.PedidoVacioException;
import com.slamshoes.exception.RecursoNoEncontradoException;
import com.slamshoes.exception.StockInsuficienteException;
import com.slamshoes.exception.ValidacionException;
import com.slamshoes.model.Pedido;
import com.slamshoes.model.PedidoDetalle;
import com.slamshoes.model.Producto;
import com.slamshoes.model.ProductoTalla;
import com.slamshoes.model.enums.EstadoPedido;
import com.slamshoes.repository.CarritoDetalleRepository;
import com.slamshoes.repository.CarritoRepository;
import com.slamshoes.repository.PedidoDetalleRepository;
import com.slamshoes.repository.PedidoRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.ProductoTallaRepository;
import com.slamshoes.repository.UsuarioRepository;

/*REFACTOR: Remove Setting Method
RAZON: Antes ServicioPedidosJpa tenía 8 campos @Autowired privados que solo Spring podía llenar. Ahora son campos final asignados en el constructor: se pueden crear con new en una prueba y nunca quedan a medias.*/
/**
 * Reglas de negocio de los pedidos: validar usuario y stock, calcular precios con la oferta vigente,
 * guardar pedido y detalle, descontar existencias y vaciar el carrito. Todo en UNA transaccion:
 * si algo falla se revierte y no se guarda nada a medias.
 *
 * Los errores se reportan con excepciones tipadas (ValidacionException, RecursoNoEncontradoException,
 * etc.) que GlobalExceptionHandler convierte a HTTP.
 *
 * Requisitos: RF-16, RF-17, RF-27, RF-28, RF-29, RF-30, RF-31.
 */
@Service
@Transactional
public class PedidoService {

    private static final int CANTIDAD_MAXIMA = 1000;

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository pedidoDetalleRepository;
    private final ProductoTallaRepository productoTallaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final CarritoDetalleRepository carritoDetalleRepository;
    private final OfertaService ofertaService;

    public PedidoService(PedidoRepository pedidoRepository,
                         PedidoDetalleRepository pedidoDetalleRepository,
                         ProductoTallaRepository productoTallaRepository,
                         ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository,
                         CarritoRepository carritoRepository,
                         CarritoDetalleRepository carritoDetalleRepository,
                         OfertaService ofertaService) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoDetalleRepository = pedidoDetalleRepository;
        this.productoTallaRepository = productoTallaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.carritoDetalleRepository = carritoDetalleRepository;
        this.ofertaService = ofertaService;
    }

    // ------------------------------------------------------------------ cliente

    /**
     * Confirma una compra: valida, reserva el stock, guarda pedido y detalle y vacía el carrito.
     *
     * @param req datos del pedido (usuario, dirección, método de pago e items)
     * @return el pedido guardado
     */
    public Pedido crearPedido(PedidoRequestDTO req) {
        if (req == null) {
            throw new ValidacionException("Solicitud de pedido vacía");
        }
        Long idUsuario = req.idUsuario();
        if (idUsuario == null || !usuarioRepository.existsById(idUsuario)) {
            throw new ValidacionException("Usuario no válido");
        }
        if (req.direccionEnvio() == null || req.direccionEnvio().isBlank()) {
            throw new ValidacionException("Falta la dirección de envío");
        }
        String direccion = req.direccionEnvio().trim();
        String metodoPago = req.metodoPago() == null || req.metodoPago().isBlank() ? null : req.metodoPago().trim();
        List<ItemPedidoDTO> items = req.items();
        if (items == null || items.isEmpty()) {
            throw new PedidoVacioException("El pedido no tiene productos");
        }

        // 1) Validar todo y reservar el stock
        validarYReservarStock(items);

        // 2) Guardar pedido y detalle
        Pedido pedido = new Pedido();
        pedido.setIdUsuario(idUsuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setDireccionEnvio(direccion);
        pedido.setMetodoPago(metodoPago);
        pedido.setTotal(0.0);
        pedido = pedidoRepository.save(pedido);

        double total = construirDetallesYTotal(pedido, items);
        pedido.setTotal(Math.round(total * 100.0) / 100.0);
        pedido = pedidoRepository.save(pedido);

        // 3) Vaciar el carrito guardado en la BD
        vaciarCarrito(idUsuario);
        return pedido;
    }

    /** Historial de pedidos de un usuario, los más recientes primero. */
    @Transactional(readOnly = true)
    public List<PedidoClienteDTO> pedidosDeUsuario(Long idUsuario) {
        List<Pedido> pedidos = pedidoRepository.findByIdUsuario(idUsuario, Sort.by(Sort.Direction.DESC, "fecha"));
        Map<Long, List<LineaPedidoDTO>> lineas = armarLineas(pedidos);
        return pedidos.stream()
                .map(p -> new PedidoClienteDTO(p.getIdPedido(), p.getFecha(), p.getTotal(), p.getEstado(),
                p.getDireccionEnvio(), p.getMetodoPago(), lineas.get(p.getIdPedido()).stream()
                    .map(l -> new PedidoClienteDTO.LineaPedidoDTO(l.idProducto(), l.nombre(),
                        l.imagenUrl(), l.talla(), l.color(), l.cantidad(), l.precioUnitario()))
                    .toList()))
                .toList();
    }

    // ------------------------------------------------------------------ panel del administrador

    /**
     * Pedidos de un estado con el nombre del cliente. En proceso: los más antiguos primero
     * (se atienden en orden); concretados: los más recientes primero.
     */
    @Transactional(readOnly = true)
    public List<PedidoFilaDTO> listarAdmin(EstadoPedido estado) {
        if (estado == null) {
            throw new ValidacionException("El parametro 'estado' debe ser PENDIENTE o CONCRETADO");
        }
        Sort orden = Sort.by(estado == EstadoPedido.CONCRETADO ? Sort.Direction.DESC : Sort.Direction.ASC, "fecha");
        List<Pedido> pedidos = pedidoRepository.findAll(orden).stream()
                .filter(p -> p.getEstado() == estado)
                .toList();

        Map<Long, List<LineaPedidoDTO>> lineas = armarLineas(pedidos);
        Map<Long, String> nombres = new HashMap<>();
        List<Long> idsUsuario = pedidos.stream().map(Pedido::getIdUsuario).distinct().toList();
        usuarioRepository.findAllById(idsUsuario).forEach(u -> nombres.put(u.getIdUsuario(), u.getNombre()));

        return pedidos.stream()
                .map(p -> new PedidoFilaDTO(p.getIdPedido(), p.getFecha(), p.getTotal(), p.getEstado(),
                        p.getDireccionEnvio(), p.getMetodoPago(), p.getIdUsuario(),
                        nombres.getOrDefault(p.getIdUsuario(), "Cliente #" + p.getIdUsuario()),
                    lineas.get(p.getIdPedido()).stream()
                        .map(l -> new PedidoClienteDTO.LineaPedidoDTO(l.idProducto(), l.nombre(),
                            l.imagenUrl(), l.talla(), l.color(), l.cantidad(), l.precioUnitario()))
                        .toList()))
                .toList();
    }

    /** Cuántos pedidos hay por estado (todos los estados aparecen, aunque sea con 0). */
    @Transactional(readOnly = true)
    public Map<EstadoPedido, Long> conteo() {
        Map<EstadoPedido, Long> conteo = new EnumMap<>(EstadoPedido.class);
        for (EstadoPedido estado : EstadoPedido.values()) {
            conteo.put(estado, 0L);
        }
        pedidoRepository.findAll().forEach(p -> {
            if (p.getEstado() != null) {
                conteo.merge(p.getEstado(), 1L, Long::sum);
            }
        });
        return conteo;
    }

    /** PENDIENTE -> CONCRETADO. El stock descontado se queda descontado. */
    public void concretar(Long idPedido) {
        Pedido pedido = buscarPedido(idPedido);
        pedido.concretar();
        pedidoRepository.save(pedido);
    }

    /** CONCRETADO -> PENDIENTE (corrige un error de dedo). */
    public void reabrir(Long idPedido) {
        Pedido pedido = buscarPedido(idPedido);
        pedido.reabrir();
        pedidoRepository.save(pedido);
    }

    /** "No concretado": borra un pedido en proceso y devuelve a la base de datos el stock descontado. */
    public void noConcretado(Long idPedido) {
        Pedido pedido = buscarPedido(idPedido);
        pedido.marcarNoConcretado();
        List<PedidoDetalle> detalles = pedidoDetalleRepository.findByIdPedido(pedido.getIdPedido());
        for (PedidoDetalle d : detalles) {
            ProductoTalla variante = productoTallaRepository.findById(d.getIdVariante()).orElse(null);
            if (variante != null) {
                Integer stock = variante.getStock();
                int actual = stock == null ? 0 : stock;
                variante.setStock(actual + d.getCantidad());
                productoTallaRepository.save(variante);
            }
        }
        pedidoDetalleRepository.deleteAll(detalles);
        pedidoRepository.delete(pedido);
    }

    // ------------------------------------------------------------------ privados

    /**
     * Valida TODAS las líneas (talla existente, producto activo, stock suficiente) y solo si todas
     * son válidas descuenta el stock. Así nunca se reserva a medias.
     */
    private void validarYReservarStock(List<ItemPedidoDTO> items) {
        List<ProductoTalla> variantes = new ArrayList<>();
        List<Integer> unidades = new ArrayList<>();

        for (Map.Entry<Long, Integer> linea : agruparPorVariante(items).entrySet()) {
            ProductoTalla variante = productoTallaRepository.findById(linea.getKey())
                    .orElseThrow(() -> new ValidacionException("Una de las tallas seleccionadas ya no existe"));
            Producto producto = productoRepository.findById(variante.getIdProducto()).orElse(null);
            if (producto == null || Boolean.FALSE.equals(producto.getActivo())) {
                throw new ValidacionException("Un producto del carrito ya no está disponible");
            }
            Integer stockDisponible = variante.getStock();
            int stock = stockDisponible == null ? 0 : stockDisponible;
            if (stock < linea.getValue()) {
                throw new StockInsuficienteException(
                        "Sin stock suficiente de \"" + producto.getNombre() + "\" talla " + variante.getTalla()
                                + " (disponibles: " + stock + ")");
            }
            variantes.add(variante);
            unidades.add(linea.getValue());
        }

        for (int i = 0; i < variantes.size(); i++) {
            ProductoTalla variante = variantes.get(i);
            variante.setStock(variante.getStock() - unidades.get(i));
            productoTallaRepository.save(variante);
        }
    }

    /**
     * Guarda un detalle por talla con el precio final (oferta vigente aplicada) y devuelve el total.
     * Recibe el pedido ya guardado porque el detalle necesita su idPedido.
     */
    private double construirDetallesYTotal(Pedido pedido, List<ItemPedidoDTO> items) {
        double total = 0;
        for (Map.Entry<Long, Integer> linea : agruparPorVariante(items).entrySet()) {
            ProductoTalla variante = productoTallaRepository.findById(linea.getKey())
                    .orElseThrow(() -> new ValidacionException("Una de las tallas seleccionadas ya no existe"));
            Producto producto = productoRepository.findById(variante.getIdProducto())
                    .orElseThrow(() -> new ValidacionException("Un producto del carrito ya no está disponible"));
            Double precioProducto = producto.getPrecio();
            double precio = ofertaService.precioFinal(
                    precioProducto == null ? 0d : precioProducto,
                    ofertaService.mejorDeProducto(producto.getIdProducto()).orElse(null));

            PedidoDetalle detalle = new PedidoDetalle();
            detalle.setIdPedido(pedido.getIdPedido());
            detalle.setIdVariante(variante.getIdVariante());
            detalle.setCantidad(linea.getValue());
            detalle.setPrecioUnitario(precio);
            pedidoDetalleRepository.save(detalle);

            total += precio * linea.getValue();
        }
        return total;
    }

    private void vaciarCarrito(Long idUsuario) {
        carritoRepository.findByIdUsuario(idUsuario)
                .ifPresent(c -> carritoDetalleRepository.deleteByIdCarrito(c.getIdCarrito()));
    }

    /** Si la misma talla viene en dos líneas se suman, para validar el stock sobre el total. */
    private static Map<Long, Integer> agruparPorVariante(List<ItemPedidoDTO> items) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemPedidoDTO item : items) {
            if (item == null || item.idVariante() == null || item.cantidad() == null) {
                throw new ValidacionException("Producto o cantidad no válidos");
            }
            int cantidad = item.cantidad();
            if (cantidad <= 0 || cantidad > CANTIDAD_MAXIMA) {
                throw new ValidacionException("Producto o cantidad no válidos (cantidad recibida: " + cantidad + ")");
            }
            cantidades.merge(item.idVariante(), cantidad, Integer::sum);
        }
        return cantidades;
    }

    /**
     * Arma las líneas (nombre, foto, talla, color, cantidad, precio) de cada pedido.
     * Detalles, tallas y productos se leen en bloque, no una consulta por línea.
     */
    private Map<Long, List<LineaPedidoDTO>> armarLineas(List<Pedido> pedidos) {
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

        Map<Long, List<LineaPedidoDTO>> resultado = new HashMap<>();
        for (Pedido p : pedidos) {
            List<LineaPedidoDTO> lineas = new ArrayList<>();
            for (PedidoDetalle d : detallesPorPedido.get(p.getIdPedido())) {
                ProductoTalla v = variantes.get(d.getIdVariante());
                Producto prod = v == null ? null : productos.get(v.getIdProducto());
                lineas.add(new LineaPedidoDTO(
                        prod == null ? null : prod.getIdProducto(),
                        prod == null ? "Producto no disponible" : prod.getNombre(),
                        prod == null ? null : prod.getImagenUrl(),
                        v == null ? null : v.getTalla(),
                        v == null ? null : v.getColor(),
                        d.getCantidad(),
                        d.getPrecioUnitario()));
            }
            resultado.put(p.getIdPedido(), lineas);
        }
        return resultado;
    }

    private Pedido buscarPedido(Long idPedido) {
        return pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido #" + idPedido));
    }
}
