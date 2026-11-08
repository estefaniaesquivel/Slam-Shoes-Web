package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.Banner;
import com.slamshoes.repository.BannerRepository;

@RestController
@RequestMapping("/api/banners")
@CrossOrigin(origins = "*")
public class BannerController {

    @Autowired
    private BannerRepository bannerRepository;

    // Banners activos del carrusel de inicio, en el orden definido por la columna "orden"
    @GetMapping
    public List<Banner> listarActivos() {
        return bannerRepository.findByActivoTrueOrderByOrdenAsc();
    }
}
