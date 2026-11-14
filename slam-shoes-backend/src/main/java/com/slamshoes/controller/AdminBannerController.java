package com.slamshoes.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.dto.BannerAdminDTO;
import com.slamshoes.model.Banner;
import com.slamshoes.repository.BannerRepository;
import com.slamshoes.service.SupabaseStorageService;

// Administración de los banners del carrusel de inicio (imágenes de 2400x800 px en Storage, carpeta "banners")
@RestController
@RequestMapping("/api/admin/banners")
@CrossOrigin(origins = "*")
public class AdminBannerController {

    private static final String CARPETA = "banners";

    @Autowired private BannerRepository bannerRepository;
    @Autowired private SupabaseStorageService storage;

    // Todos los banners (activos o no), en orden
    @GetMapping
    public List<Banner> listar() {
        return bannerRepository.findAll(Sort.by("orden").ascending().and(Sort.by("idBanner").ascending()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(@RequestPart("banner") BannerAdminDTO datos,
                                   @RequestPart("imagen") MultipartFile imagen) {
        if (datos.titulo() == null || datos.titulo().isBlank()) return error("El título del banner es obligatorio");
        if (!SupabaseStorageService.esWebp(imagen)) return error("Sube la imagen del banner en formato .webp");
        if (!storage.estaConfigurado()) return sinLlave();

        String url;
        try {
            url = storage.subirWebp(CARPETA, datos.titulo(), imagen);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo subir la imagen a Supabase: " + e.getMessage()));
        }
        try {
            Banner b = new Banner();
            aplicar(b, datos);
            if (b.getOrden() == null) {
                b.setOrden(bannerRepository.findAll().stream()
                        .map(x -> x.getOrden() == null ? 0 : x.getOrden()).max(Integer::compare).orElse(0) + 1);
            }
            b.setImagenUrl(url);
            return ResponseEntity.status(201).body(bannerRepository.save(b));
        } catch (RuntimeException e) {
            storage.eliminarPorUrl(url);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo guardar el banner: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @RequestPart("banner") BannerAdminDTO datos,
                                        @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        Banner b = bannerRepository.findById(id).orElse(null);
        if (b == null) return ResponseEntity.notFound().build();
        if (datos.titulo() == null || datos.titulo().isBlank()) return error("El título del banner es obligatorio");

        boolean hayImagenNueva = imagen != null && !imagen.isEmpty();
        if (hayImagenNueva && !SupabaseStorageService.esWebp(imagen)) return error("La imagen debe estar en formato .webp");
        if (hayImagenNueva && !storage.estaConfigurado()) return sinLlave();

        String urlAnterior = b.getImagenUrl();
        String urlNueva = null;
        try {
            if (hayImagenNueva) urlNueva = storage.subirWebp(CARPETA, datos.titulo(), imagen);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo subir la imagen a Supabase: " + e.getMessage()));
        }
        try {
            aplicar(b, datos);
            if (urlNueva != null) b.setImagenUrl(urlNueva);
            Banner guardado = bannerRepository.save(b);
            if (urlNueva != null) storage.eliminarPorUrl(urlAnterior); // la imagen reemplazada ya no se usa
            return ResponseEntity.ok(guardado);
        } catch (RuntimeException e) {
            if (urlNueva != null) storage.eliminarPorUrl(urlNueva);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo actualizar el banner: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        Banner b = bannerRepository.findById(id).orElse(null);
        if (b == null) return ResponseEntity.notFound().build();
        bannerRepository.delete(b);
        storage.eliminarPorUrl(b.getImagenUrl());
        return ResponseEntity.noContent().build();
    }

    private void aplicar(Banner b, BannerAdminDTO d) {
        b.setTitulo(d.titulo().trim());
        String enlace = d.enlaceDestino() == null ? "" : d.enlaceDestino().trim();
        b.setEnlaceDestino(enlace.isEmpty() ? null : enlace);
        if (d.orden() != null) b.setOrden(d.orden());
        b.setActivo(d.activo() == null || d.activo());
    }

    private ResponseEntity<Map<String, String>> error(String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }

    private ResponseEntity<Map<String, String>> sinLlave() {
        return ResponseEntity.status(500).body(Map.of("error",
                "Falta configurar la llave de Supabase en el backend (application-secrets.properties) para subir imágenes"));
    }
}
