package com.slamshoes.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Oferta;
import com.slamshoes.repository.OfertaRepository;

@RestController
@RequestMapping("/api/ofertas")
@CrossOrigin(origins = "*")
public class OfertaController {

    @Autowired
    private OfertaRepository ofertaRepository;

    // Ofertas vigentes en este momento (activas y dentro de su periodo)
    @GetMapping("/vigentes")
    public List<Oferta> listarVigentes() {
        LocalDateTime ahora = LocalDateTime.now();
        return ofertaRepository
                .findByActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(ahora, ahora);
    }
}
