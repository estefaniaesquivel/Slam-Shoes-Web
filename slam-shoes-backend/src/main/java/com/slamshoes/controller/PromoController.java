package com.slamshoes.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Novedad;
import com.slamshoes.model.Oferta;
import com.slamshoes.repository.NovedadRepository;
import com.slamshoes.service.OfertaService;

@RestController
@RequestMapping("/api/promos")
@CrossOrigin(origins = "*")
public class PromoController {

    @Autowired private OfertaService ofertaService;
    @Autowired private NovedadRepository novedadRepository;

    // Portadas de los bloques "Novedades" y "Ofertas" del inicio:
    // se toma la primera portada_url no vacía entre las filas vigentes (null si no hay)
    @GetMapping("/portadas")
    public Map<String, String> portadas() {
        LocalDateTime ahora = LocalDateTime.now();
        Map<String, String> resultado = new HashMap<>();

        resultado.put("ofertas", ofertaService.vigentes().stream()
                .map(Oferta::getPortadaUrl)
                .filter(u -> u != null && !u.isBlank())
                .findFirst().orElse(null));

        resultado.put("novedades", novedadRepository
                .findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaInicioDesc(ahora, ahora).stream()
                .map(Novedad::getPortadaUrl)
                .filter(u -> u != null && !u.isBlank())
                .findFirst().orElse(null));

        return resultado;
    }
}
