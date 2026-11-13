package com.slamshoes.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.dto.OfertaProductoDTO;
import com.slamshoes.dto.OfertaResumenDTO;
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
                o.getIdOferta(), o.getNombre(), o.getPorcentajeDescuento(),
                o.getFechaInicio(), o.getFechaFin(), ofertaService.esVigente(o),
                ofertaService.precioFinal(p.getPrecio() == null ? 0 : p.getPrecio(), o), p);
    }

    // Ofertas vigentes en bruto
    @GetMapping("/vigentes")
    public List<Oferta> listarVigentes() {
        return ofertaService.vigentes();
    }

    // TODAS las ofertas activas que no han terminado (vigentes hoy y programadas), un producto por oferta
    @GetMapping("/productos")
    public List<OfertaProductoDTO> productosEnOferta() {
        return ofertaService.mejorVisiblePorProducto().values().stream()
                .map(o -> productoRepository.findById(o.getIdProducto())
                        .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                        .map(p -> aDTO(o, p))
                        .orElse(null))
                .filter(dto -> dto != null)
                .sorted(Comparator.comparing(OfertaProductoDTO::vigente).reversed()
                        .thenComparing(Comparator.comparing(OfertaProductoDTO::porcentajeDescuento).reversed()))
                .toList();
    }

    // Mejor oferta de cada producto en ligero: sirve para pintar la etiqueta de descuento en todas las tarjetas
    @GetMapping("/resumen")
    public List<OfertaResumenDTO> resumen() {
        return ofertaService.mejorVisiblePorProducto().values().stream()
                .map(o -> {
                    Double precio = productoRepository.findById(o.getIdProducto())
                            .map(Producto::getPrecio).orElse(0.0);
                    return new OfertaResumenDTO(
                            o.getIdProducto(), o.getNombre(), o.getPorcentajeDescuento(),
                            ofertaService.precioFinal(precio == null ? 0 : precio, o),
                            o.getFechaInicio(), o.getFechaFin(), ofertaService.esVigente(o));
                })
                .toList();
    }

    // Oferta (vigente o programada) de un producto (404 si no tiene)
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<OfertaProductoDTO> ofertaDeProducto(@PathVariable Long idProducto) {
        Oferta oferta = ofertaService.mejorVisiblePorProducto().get(idProducto);
        if (oferta == null) return ResponseEntity.notFound().build();
        return productoRepository.findById(idProducto)
                .map(p -> ResponseEntity.ok(aDTO(oferta, p)))
                .orElse(ResponseEntity.notFound().build());
    }
}
