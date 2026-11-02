package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.ProductoTalla;
import com.slamshoes.repository.ProductoTallaRepository;

@RestController
@RequestMapping("/api/producto-tallas")
@CrossOrigin(origins = "*")
public class ProductoTallaController {

    @Autowired
    private ProductoTallaRepository repository;

    @GetMapping("/producto/{idProducto}")
    public List<ProductoTalla> obtenerPorProducto(@PathVariable Long idProducto) {
        return repository.findByIdProducto(idProducto);
    }
}