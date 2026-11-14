package com.slamshoes.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.dto.OfertaAdminDTO;
import com.slamshoes.model.Oferta;
import com.slamshoes.repository.OfertaRepository;
import com.slamshoes.repository.ProductoRepository;

// Administración de ofertas: crear, editar y quitar la oferta de un producto (nunca borra el producto)
@RestController
@RequestMapping("/api/admin/ofertas")
@CrossOrigin(origins = "*")
public class AdminOfertaController {

    @Autowired private OfertaRepository ofertaRepository;
    @Autowired private ProductoRepository productoRepository;

    // Todas las ofertas (vigentes, programadas y vencidas)
    @GetMapping
    public List<Oferta> listar() {
        return ofertaRepository.findAll(Sort.by("idOferta").ascending());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody OfertaAdminDTO d) {
        if (d.idProducto() == null || !productoRepository.existsById(d.idProducto())) {
            return error("Selecciona un producto válido");
        }
        String problema = validar(d);
        if (problema != null) return error(problema);

        Oferta o = new Oferta();
        o.setIdProducto(d.idProducto());
        o.setActiva(true);
        aplicar(o, d);
        return ResponseEntity.status(201).body(ofertaRepository.save(o));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody OfertaAdminDTO d) {
        Oferta o = ofertaRepository.findById(id).orElse(null);
        if (o == null) return ResponseEntity.notFound().build();
        String problema = validar(d);
        if (problema != null) return error(problema);
        aplicar(o, d);
        return ResponseEntity.ok(ofertaRepository.save(o));
    }

    // Quita solo la oferta; el producto no se toca
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!ofertaRepository.existsById(id)) return ResponseEntity.notFound().build();
        ofertaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String validar(OfertaAdminDTO d) {
        if (d.nombre() == null || d.nombre().isBlank()) return "Escribe el nombre de la oferta";
        if (d.porcentajeDescuento() == null || d.porcentajeDescuento() <= 0 || d.porcentajeDescuento() >= 100) {
            return "El descuento debe estar entre 1 y 99 %";
        }
        if (d.fechaInicio() == null || d.fechaFin() == null) return "Indica la fecha de inicio y la fecha de fin";
        if (!d.fechaFin().isAfter(d.fechaInicio())) return "La fecha de fin debe ser posterior a la de inicio";
        return null;
    }

    private void aplicar(Oferta o, OfertaAdminDTO d) {
        o.setNombre(d.nombre().trim());
        o.setPorcentajeDescuento(d.porcentajeDescuento());
        o.setFechaInicio(d.fechaInicio());
        o.setFechaFin(d.fechaFin());
    }

    private ResponseEntity<Map<String, String>> error(String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }
}
