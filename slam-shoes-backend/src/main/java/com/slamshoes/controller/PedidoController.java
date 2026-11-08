package com.slamshoes.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.dto.ItemPedidoDTO;
import com.slamshoes.dto.PedidoClienteDTO;
import com.slamshoes.dto.PedidoRequestDTO;
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

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PedidoDetalleRepository pedidoDetalleRepository;
    @Autowired private ProductoTallaRepository productoTallaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CarritoRepository carritoRepository;
    @Autowired private CarritoDetalleRepository carritoDetalleRepository;
    @Autowired private OfertaService ofertaService;

    // Confirmar compra: llena pedidos y pedido_detalle con precios y total calculados en el servidor
    @PostMapping
    @Transactional
    public ResponseEntity<?> crearPedido(@RequestBody PedidoRequestDTO req) {
        if (req == null || req.idUsuario() == null || !usuarioRepository.existsById(req.idUsuario())) {
            return error("Usuario no válido");
        }
        if (req.direccionEnvio() == null || req.direccionEnvio().isBlank()) {
            return error("Falta la dirección de envío");
        }
        if (req.items() == null || req.items().isEmpty()) {
            return error("El pedido no tiene productos");
        }

        // 1) Validar todo antes de guardar nada
        List<ProductoTalla> variantes = new ArrayList<>();
        List<Double> precios = new ArrayList<>();
        List<Integer> cantidades = new ArrayList<>();
        double total = 0;

        for (ItemPedidoDTO item : req.items()) {
            if (item.idVariante() == null || item.cantidad() == null || item.cantidad() <= 0) {
                return error("Producto o cantidad no válidos");
            }
            ProductoTalla variante = productoTallaRepository.findById(item.idVariante()).orElse(null);
            if (variante == null) {
                return error("Una de las tallas seleccionadas ya no existe");
            }
            Producto producto = productoRepository.findById(variante.getIdProducto()).orElse(null);
            if (producto == null || Boolean.FALSE.equals(producto.getActivo())) {
                return error("Un producto del carrito ya no está disponible");
            }
            int stock = variante.getStock() == null ? 0 : variante.getStock();
            if (stock < item.cantidad()) {
                return error("Sin stock suficiente de \"" + producto.getNombre() + "\" talla " + variante.getTalla());
            }

            double precio = ofertaService.precioFinal(
                    producto.getPrecio() == null ? 0 : producto.getPrecio(),
                    ofertaService.mejorDeProducto(producto.getIdProducto()).orElse(null));

            variantes.add(variante);
            precios.add(precio);
            cantidades.add(item.cantidad());
            total += precio * item.cantidad();
        }

        // 2) Guardar pedido y detalle
        Pedido pedido = new Pedido();
        pedido.setIdUsuario(req.idUsuario());
        pedido.setFecha(LocalDateTime.now());
        pedido.setTotal(Math.round(total * 100.0) / 100.0);
        pedido.setEstado("PENDIENTE");
        pedido.setDireccionEnvio(req.direccionEnvio().trim());
        pedido.setMetodoPago(req.metodoPago() == null || req.metodoPago().isBlank() ? null : req.metodoPago());
        pedido = pedidoRepository.save(pedido);

        for (int i = 0; i < variantes.size(); i++) {
            ProductoTalla variante = variantes.get(i);

            PedidoDetalle detalle = new PedidoDetalle();
            detalle.setIdPedido(pedido.getIdPedido());
            detalle.setIdVariante(variante.getIdVariante());
            detalle.setCantidad(cantidades.get(i));
            detalle.setPrecioUnitario(precios.get(i));
            pedidoDetalleRepository.save(detalle);

            variante.setStock(variante.getStock() - cantidades.get(i));
            productoTallaRepository.save(variante);
        }

        // 3) Vaciar el carrito guardado en BD
        final Long idPedido = pedido.getIdPedido();
        carritoRepository.findByIdUsuario(req.idUsuario())
                .ifPresent(c -> carritoDetalleRepository.deleteByIdCarrito(c.getIdCarrito()));

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("idPedido", idPedido);
        respuesta.put("total", pedido.getTotal());
        respuesta.put("estado", pedido.getEstado());
        return ResponseEntity.status(201).body(respuesta);
    }

    // Mis pedidos: pedidos del cliente con sus productos (más recientes primero)
    @GetMapping("/usuario/{idUsuario}")
    public List<PedidoClienteDTO> pedidosDeUsuario(@PathVariable Long idUsuario) {
        return pedidoRepository.findByIdUsuario(idUsuario, Sort.by(Sort.Direction.DESC, "fecha")).stream()
                .map(p -> new PedidoClienteDTO(
                        p.getIdPedido(), p.getFecha(), p.getTotal(), p.getEstado(),
                        p.getDireccionEnvio(), p.getMetodoPago(),
                        pedidoDetalleRepository.findByIdPedido(p.getIdPedido()).stream().map(d -> {
                            ProductoTalla v = productoTallaRepository.findById(d.getIdVariante()).orElse(null);
                            Producto prod = v == null ? null : productoRepository.findById(v.getIdProducto()).orElse(null);
                            return new PedidoClienteDTO.LineaPedidoDTO(
                                    prod == null ? null : prod.getIdProducto(),
                                    prod == null ? "Producto no disponible" : prod.getNombre(),
                                    prod == null ? null : prod.getImagenUrl(),
                                    v == null ? null : v.getTalla(),
                                    v == null ? null : v.getColor(),
                                    d.getCantidad(), d.getPrecioUnitario());
                        }).toList()))
                .toList();
    }

    private ResponseEntity<Map<String, String>> error(String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }
}
