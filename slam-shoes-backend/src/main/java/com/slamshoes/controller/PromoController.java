package com.slamshoes.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Novedad;
import com.slamshoes.model.Oferta;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.NovedadRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.service.OfertaService;

/**
 * Resumen de los bloques "Novedades" y "Ofertas" del inicio.
 * Ya no usa ninguna columna portada_url: la portada de cada bloque es la imagen
 * (imagen_url) del primer producto activo que está vigente en ese bloque.
 */
@RestController
@RequestMapping("/api/promos")
@CrossOrigin(origins = "*")
public class PromoController {

    @Autowired private NovedadRepository novedadRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private OfertaService ofertaService;

    // GET /api/promos/portadas
    // { "novedades": { "portada": "https://...", "total": 3 }, "ofertas": { "portada": null, "total": 0 } }
    @GetMapping("/portadas")
    public Map<String, Map<String, Object>> portadas() {
        LocalDateTime ahora = LocalDateTime.now();

        // Novedades vigentes (más recientes primero)
        List<Long> idsNovedades = novedadRepository
                .findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaInicioDesc(ahora, ahora)
                .stream().map(Novedad::getIdProducto).distinct().toList();

        // Ofertas visibles (vigentes y programadas): primero las vigentes, luego mayor descuento
        List<Long> idsOfertas = ofertaService.mejorVisiblePorProducto().values().stream()
                .sorted((a, b) -> {
                    int porVigencia = Boolean.compare(ofertaService.esVigente(b), ofertaService.esVigente(a));
                    return porVigencia != 0 ? porVigencia
                            : Double.compare(b.getPorcentajeDescuento(), a.getPorcentajeDescuento());
                })
                .map(Oferta::getIdProducto).toList();

        Map<String, Map<String, Object>> resultado = new LinkedHashMap<>();
        resultado.put("novedades", bloque(idsNovedades));
        resultado.put("ofertas", bloque(idsOfertas));
        return resultado;
    }

    // Una sola consulta por bloque (findAllById) en lugar de una por producto
    private Map<String, Object> bloque(List<Long> idsEnOrden) {
        Map<Long, Producto> activos = new LinkedHashMap<>();
        productoRepository.findAllById(idsEnOrden).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .forEach(p -> activos.put(p.getIdProducto(), p));

        Optional<String> portada = idsEnOrden.stream()
                .map(activos::get)
                .filter(p -> p != null && p.getImagenUrl() != null && !p.getImagenUrl().isBlank())
                .map(Producto::getImagenUrl)
                .findFirst();

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("portada", portada.orElse(null));
        datos.put("total", activos.size());
        return datos;
    }
}
