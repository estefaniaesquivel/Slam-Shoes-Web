package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.slamshoes.repository.FavoritoRepository;

@RestController
@RequestMapping("/api/favoritos")
@CrossOrigin(origins = "*")
public class FavoritoController {

    @Autowired
    private FavoritoRepository favoritoRepository;

    @GetMapping("/usuario/{idUsuario}")
    public List<Favorito> listarFavoritosPorUsuario(@PathVariable Long idUsuario) {
        return favoritoRepository.findByIdUsuario(idUsuario);
    }

    @PostMapping
    public Favorito agregarFavorito(@RequestBody Favorito favorito) {
        return favoritoRepository.save(favorito);
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminarFavorito(@RequestParam Long idUsuario, @RequestParam Long idProducto) {
        favoritoRepository.deleteByIdUsuarioAndIdProducto(idUsuario, idProducto);
        return ResponseEntity.noContent().build();
    }
}