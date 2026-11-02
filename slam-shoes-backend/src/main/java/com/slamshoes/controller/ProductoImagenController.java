package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.ProductoImagen;
import com.slamshoes.repository.ProductoImagenRepository;

@RestController
@RequestMapping("/api/producto-imagenes")
@CrossOrigin(origins = "*")
public class ProductoImagenController {

    @Autowired
    private ProductoImagenRepository repository;

    @GetMapping("/producto/{idProducto}")
    public List<ProductoImagen> obtenerPorProducto(@PathVariable Long idProducto) {
        return repository.findByIdProducto(idProducto);
    }
}