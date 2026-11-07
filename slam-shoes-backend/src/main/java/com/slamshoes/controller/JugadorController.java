package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Jugador;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.JugadorRepository;
import com.slamshoes.repository.ProductoRepository;

@RestController
@RequestMapping("/api/jugadores")
@CrossOrigin(origins = "*")
public class JugadorController {

    @Autowired
    private JugadorRepository jugadorRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // 1. Lista de jugadores de la tabla "jugadores" (alimenta el menú "Por Jugador")
    @GetMapping
    public List<Jugador> listarJugadores() {
        return jugadorRepository.findAllByOrderByNombreAsc();
    }

    // 2. Un jugador (nombre e imagen_url del banner de su página)
    @GetMapping("/{id}")
    public ResponseEntity<Jugador> obtenerJugador(@PathVariable Long id) {
        return jugadorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Catálogo de tenis indexados al jugador (productos.id_jugador); se omiten los desactivados
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<Producto>> listarProductosDeJugador(@PathVariable Long id) {
        if (!jugadorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<Producto> productos = productoRepository.findByIdJugador(id).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .toList();
        return ResponseEntity.ok(productos);
    }
}
