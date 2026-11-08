package com.slamshoes.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Novedad;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.NovedadRepository;
import com.slamshoes.repository.ProductoRepository;

@RestController
@RequestMapping("/api/novedades")
@CrossOrigin(origins = "*")
public class NovedadController {

    @Autowired
    private NovedadRepository novedadRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // Productos que son novedad en este momento (hoy dentro de fecha_inicio - fecha_fin)
    @GetMapping("/productos")
    public List<Producto> productosNovedad() {
        LocalDateTime ahora = LocalDateTime.now();
        return novedadRepository
                .findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaInicioDesc(ahora, ahora)
                .stream()
                .map(Novedad::getIdProducto)
                .distinct()
                .map(id -> productoRepository.findById(id).orElse(null))
                .filter(p -> p != null && !Boolean.FALSE.equals(p.getActivo()))
                .toList();
    }
}
