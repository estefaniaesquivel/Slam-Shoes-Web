package com.slamshoes.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        List<Long> ids = novedadRepository
                .findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaInicioDesc(ahora, ahora)
                .stream()
                .map(Novedad::getIdProducto)
                .distinct()
                .toList();
        // Una sola consulta para todos los productos (antes: una por novedad), conservando el orden
        Map<Long, Producto> porId = productoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Producto::getIdProducto, Function.identity()));
        return ids.stream()
                .map(porId::get)
                .filter(p -> p != null && !Boolean.FALSE.equals(p.getActivo()))
                .toList();
    }
}
