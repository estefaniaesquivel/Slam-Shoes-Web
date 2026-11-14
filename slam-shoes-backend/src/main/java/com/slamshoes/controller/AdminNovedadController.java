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

import com.slamshoes.dto.NovedadAdminDTO;
import com.slamshoes.model.Novedad;
import com.slamshoes.repository.NovedadRepository;
import com.slamshoes.repository.ProductoRepository;

// Administración de novedades: marcar un producto como novedad por un periodo, editarlo o descartarlo
@RestController
@RequestMapping("/api/admin/novedades")
@CrossOrigin(origins = "*")
public class AdminNovedadController {

    @Autowired private NovedadRepository novedadRepository;
    @Autowired private ProductoRepository productoRepository;

    @GetMapping
    public List<Novedad> listar() {
        return novedadRepository.findAll(Sort.by("idNovedad").ascending());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody NovedadAdminDTO d) {
        if (d.idProducto() == null || !productoRepository.existsById(d.idProducto())) {
            return error("Selecciona un producto válido");
        }
        String problema = validar(d);
        if (problema != null) return error(problema);

        Novedad n = new Novedad();
        n.setIdProducto(d.idProducto());
        n.setFechaInicio(d.fechaInicio());
        n.setFechaFin(d.fechaFin());
        return ResponseEntity.status(201).body(novedadRepository.save(n));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody NovedadAdminDTO d) {
        Novedad n = novedadRepository.findById(id).orElse(null);
        if (n == null) return ResponseEntity.notFound().build();
        String problema = validar(d);
        if (problema != null) return error(problema);
        n.setFechaInicio(d.fechaInicio());
        n.setFechaFin(d.fechaFin());
        return ResponseEntity.ok(novedadRepository.save(n));
    }

    // Descarta la novedad; el producto no se toca
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!novedadRepository.existsById(id)) return ResponseEntity.notFound().build();
        novedadRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String validar(NovedadAdminDTO d) {
        if (d.fechaInicio() == null || d.fechaFin() == null) return "Indica la fecha de inicio y la fecha de fin";
        if (!d.fechaFin().isAfter(d.fechaInicio())) return "La fecha de fin debe ser posterior a la de inicio";
        return null;
    }

    private ResponseEntity<Map<String, String>> error(String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }
}
