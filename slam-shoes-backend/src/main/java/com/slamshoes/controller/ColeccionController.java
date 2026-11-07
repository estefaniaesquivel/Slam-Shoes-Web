package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Coleccion;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.ColeccionRepository;
import com.slamshoes.repository.ProductoRepository;

@RestController
@RequestMapping("/api/colecciones")
@CrossOrigin(origins = "*")
public class ColeccionController {

    @Autowired
    private ColeccionRepository coleccionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // Colecciones activas de la tabla "colecciones" (Home y menú del encabezado)
    @GetMapping
    public List<Coleccion> listarColecciones() {
        return coleccionRepository.findByActivaTrueOrderByNombreAsc();
    }

    // Una colección (nombre, descripción e imagen)
    @GetMapping("/{id}")
    public ResponseEntity<Coleccion> obtenerColeccion(@PathVariable Long id) {
        return coleccionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Catálogo de la colección (productos.id_coleccion); se omiten los desactivados
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<Producto>> listarProductosDeColeccion(@PathVariable Long id) {
        if (!coleccionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<Producto> productos = productoRepository.findByIdColeccion(id).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .toList();
        return ResponseEntity.ok(productos);
    }
}
