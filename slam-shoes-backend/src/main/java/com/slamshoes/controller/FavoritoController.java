package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Favorito;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.FavoritoRepository;

@RestController
@RequestMapping("/api/favoritos")
@CrossOrigin(origins = "*")
public class FavoritoController {

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping("/usuario/{idUsuario}")
    public List<Favorito> listarFavoritosPorUsuario(@PathVariable Long idUsuario) {
        return favoritoRepository.findByIdUsuario(idUsuario);
    }

    // Productos favoritos del usuario (para restaurarlos al iniciar sesión)
    @GetMapping("/usuario/{idUsuario}/productos")
    public List<Producto> listarProductosFavoritos(@PathVariable Long idUsuario) {
        return favoritoRepository.findByIdUsuario(idUsuario).stream()
                .map(f -> productoRepository.findById(f.getIdProducto()).orElse(null))
                .filter(p -> p != null)
                .toList();
    }

    @PostMapping
    public Favorito agregarFavorito(@RequestBody Favorito favorito) {
        // Evita duplicados (hay una restricción única usuario-producto)
        if (favoritoRepository.existsByIdUsuarioAndIdProducto(favorito.getIdUsuario(), favorito.getIdProducto())) {
            return favoritoRepository.findByIdUsuario(favorito.getIdUsuario()).stream()
                    .filter(f -> f.getIdProducto().equals(favorito.getIdProducto()))
                    .findFirst().orElse(favorito);
        }
        return favoritoRepository.save(favorito);
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> eliminarFavorito(@RequestParam Long idUsuario, @RequestParam Long idProducto) {
        favoritoRepository.deleteByIdUsuarioAndIdProducto(idUsuario, idProducto);
        return ResponseEntity.noContent().build();
    }
}