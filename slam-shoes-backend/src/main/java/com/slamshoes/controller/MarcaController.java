package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Marca;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.MarcaRepository;
import com.slamshoes.repository.ProductoRepository;

@RestController
@RequestMapping("/api/marcas")
@CrossOrigin(origins = "*")
public class MarcaController {

    @Autowired
    private MarcaRepository repo;

    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping
    public List<Marca> listar() {
        return repo.findByActivaTrueOrderByNombreAsc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Marca> obtener(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Catálogo: productos activos relacionados mediante productos.id_marca
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<Producto>> listarProductos(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<Producto> productos = productoRepository.findByIdMarca(id).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .toList();
        return ResponseEntity.ok(productos);
    }
}
