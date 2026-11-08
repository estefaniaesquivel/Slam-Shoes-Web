package com.slamshoes.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.dto.OfertaProductoDTO;
import com.slamshoes.model.Oferta;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.service.OfertaService;

@RestController
@RequestMapping("/api/ofertas")
@CrossOrigin(origins = "*")
public class OfertaController {

    @Autowired
    private OfertaService ofertaService;

    @Autowired
    private ProductoRepository productoRepository;

    private OfertaProductoDTO aDTO(Oferta o, Producto p) {
        return new OfertaProductoDTO(
                o.getIdOferta(), o.getNombre(), o.getPorcentajeDescuento(), o.getFechaFin(),
                ofertaService.precioFinal(p.getPrecio() == null ? 0 : p.getPrecio(), o), p);
    }

    // Ofertas vigentes en bruto
    @GetMapping("/vigentes")
    public List<Oferta> listarVigentes() {
        return ofertaService.vigentes();
    }

    // Productos en oferta vigente (opcionalmente solo los de una oferta por nombre, ej. "BUEN FIN")
    @GetMapping("/productos")
    public List<OfertaProductoDTO> productosEnOferta(@RequestParam(required = false) String nombre) {
        List<Oferta> vigentes = ofertaService.vigentes().stream()
                .filter(o -> nombre == null || nombre.isBlank()
                        || (o.getNombre() != null && o.getNombre().equalsIgnoreCase(nombre.trim())))
                .toList();

        return ofertaService.mejorPorProducto(vigentes).values().stream()
                .map(o -> productoRepository.findById(o.getIdProducto())
                        .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                        .map(p -> aDTO(o, p))
                        .orElse(null))
                .filter(dto -> dto != null)
                .sorted(Comparator.comparing(OfertaProductoDTO::porcentajeDescuento).reversed())
                .toList();
    }

    // Nombres distintos de las ofertas vigentes (para los accesos por oferta: BUEN FIN, etc.)
    @GetMapping("/nombres")
    public List<String> nombresVigentes() {
        return ofertaService.vigentes().stream()
                .map(Oferta::getNombre)
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    // Oferta vigente de un producto (404 si no tiene)
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<OfertaProductoDTO> ofertaDeProducto(@PathVariable Long idProducto) {
        return ofertaService.mejorDeProducto(idProducto)
                .flatMap(o -> productoRepository.findById(idProducto).map(p -> aDTO(o, p)))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
