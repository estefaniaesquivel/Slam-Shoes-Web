package com.slamshoes.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Carrito;
import com.slamshoes.dto.CarritoItemDTO;
import com.slamshoes.model.CarritoDetalle;
import com.slamshoes.model.Producto;
import com.slamshoes.model.ProductoTalla;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.ProductoTallaRepository;
import com.slamshoes.service.OfertaService;
import com.slamshoes.repository.CarritoDetalleRepository;
import com.slamshoes.repository.CarritoRepository;

@RestController
@RequestMapping("/api/carrito")
@CrossOrigin(origins = "*")
public class CarritoController {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private CarritoDetalleRepository carritoDetalleRepository;

    @Autowired
    private ProductoTallaRepository productoTallaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private OfertaService ofertaService;

    @GetMapping("/usuario/{idUsuario}")
    public Carrito obtenerCarritoPorUsuario(@PathVariable Long idUsuario) {
        return carritoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setIdUsuario(idUsuario);
                    nuevoCarrito.setFechaActualizacion(LocalDateTime.now());
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    // Carrito del usuario con los datos listos para mostrarse (se restaura al iniciar sesión)
    @GetMapping("/usuario/{idUsuario}/items")
    public List<CarritoItemDTO> obtenerItems(@PathVariable Long idUsuario) {
        return carritoRepository.findByIdUsuario(idUsuario)
                .map(c -> carritoDetalleRepository.findByIdCarrito(c.getIdCarrito()).stream()
                        .map(d -> {
                            ProductoTalla v = productoTallaRepository.findById(d.getIdVariante()).orElse(null);
                            if (v == null) return null;
                            Producto p = productoRepository.findById(v.getIdProducto()).orElse(null);
                            if (p == null) return null;
                            double precio = ofertaService.precioFinal(
                                    p.getPrecio() == null ? 0 : p.getPrecio(),
                                    ofertaService.mejorDeProducto(p.getIdProducto()).orElse(null));
                            return new CarritoItemDTO(p.getIdProducto(), v.getIdVariante(), p.getNombre(), precio,
                                    p.getImagenUrl(), v.getTalla(), v.getColor(), d.getCantidad());
                        })
                        .filter(i -> i != null)
                        .toList())
                .orElse(List.of());
    }

    @GetMapping("/{idCarrito}/detalles")
    public List<CarritoDetalle> obtenerDetalles(@PathVariable Long idCarrito) {
        return carritoDetalleRepository.findByIdCarrito(idCarrito);
    }

    @PostMapping("/{idCarrito}/sincronizar")
    @Transactional
    public List<CarritoDetalle> sincronizarCarrito(@PathVariable Long idCarrito, @RequestBody List<CarritoDetalle> nuevosDetalles) {
        // Limpiamos los elementos anteriores usando la consulta explícita
        carritoDetalleRepository.deleteByIdCarrito(idCarrito);

        // Guardamos los nuevos elementos activos
        for (CarritoDetalle detalle : nuevosDetalles) {
            detalle.setIdCarrito(idCarrito);
            carritoDetalleRepository.save(detalle);
        }

        carritoRepository.findById(idCarrito).ifPresent(c -> {
            c.setFechaActualizacion(LocalDateTime.now());
            carritoRepository.save(c);
        });

        return carritoDetalleRepository.findByIdCarrito(idCarrito);
    }

    @DeleteMapping("/eliminar")
    public ResponseEntity<Void> eliminarItem(@RequestParam Long idCarrito, @RequestParam Long idVariante) {
        carritoDetalleRepository.deleteByIdCarritoAndIdVariante(idCarrito, idVariante);
        return ResponseEntity.noContent().build();
    }
}