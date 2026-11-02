package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Coleccion;
import com.slamshoes.repository.ColeccionRepository;

@RestController
@RequestMapping("/api/colecciones")
@CrossOrigin(origins = "*")
public class ColeccionController {

    @Autowired
    private ColeccionRepository coleccionRepository;

    @GetMapping
    public List<Coleccion> listarColecciones() {
        return coleccionRepository.findAll();
    }
}