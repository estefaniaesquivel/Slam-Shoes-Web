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

import com.slamshoes.dto.NombreImagenAdminDTO;
import com.slamshoes.model.Jugador;
import com.slamshoes.repository.JugadorRepository;
import com.slamshoes.service.AdminCatalogoService;
import com.slamshoes.service.SupabaseStorageService;

// Administración de jugadores: imagen de 2400x800 px en Storage, carpeta "jugadores"
@RestController
@RequestMapping("/api/admin/jugadores")
@CrossOrigin(origins = "*")
public class AdminJugadorController {

    private static final String CARPETA = "jugadores";

    @Autowired private JugadorRepository repositorio;
    @Autowired private AdminCatalogoService catalogoService;
    @Autowired private SupabaseStorageService storage;

    @GetMapping
    public List<Jugador> listar() {
        return repositorio.findAll(Sort.by("nombre").ascending());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(@RequestPart("datos") NombreImagenAdminDTO datos,
                                   @RequestPart("imagen") MultipartFile imagen) {
        if (datos.nombre() == null || datos.nombre().isBlank()) return error("El nombre es obligatorio");
        if (!SupabaseStorageService.esWebp(imagen)) return error("Sube la imagen en formato .webp");
        if (!storage.estaConfigurado()) return sinLlave();

        String url;
        try {
            url = storage.subirWebp(CARPETA, datos.nombre(), imagen);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo subir la imagen a Supabase: " + e.getMessage()));
        }
        try {
            Jugador nuevo = new Jugador();
            nuevo.setNombre(datos.nombre().trim());
            nuevo.setImagenUrl(url);
            return ResponseEntity.status(201).body(repositorio.save(nuevo));
        } catch (RuntimeException e) {
            storage.eliminarPorUrl(url);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo guardar: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @RequestPart("datos") NombreImagenAdminDTO datos,
                                        @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        Jugador actual = repositorio.findById(id).orElse(null);
        if (actual == null) return ResponseEntity.notFound().build();
        if (datos.nombre() == null || datos.nombre().isBlank()) return error("El nombre es obligatorio");

        boolean hayImagenNueva = imagen != null && !imagen.isEmpty();
        if (hayImagenNueva && !SupabaseStorageService.esWebp(imagen)) return error("La imagen debe estar en formato .webp");
        if (hayImagenNueva && !storage.estaConfigurado()) return sinLlave();

        String urlAnterior = actual.getImagenUrl();
        String urlNueva = null;
        try {
            if (hayImagenNueva) urlNueva = storage.subirWebp(CARPETA, datos.nombre(), imagen);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo subir la imagen a Supabase: " + e.getMessage()));
        }
        try {
            actual.setNombre(datos.nombre().trim());
            if (urlNueva != null) actual.setImagenUrl(urlNueva);
            Jugador guardado = repositorio.save(actual);
            if (urlNueva != null) storage.eliminarPorUrl(urlAnterior);
            return ResponseEntity.ok(guardado);
        } catch (RuntimeException e) {
            if (urlNueva != null) storage.eliminarPorUrl(urlNueva);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo actualizar: " + e.getMessage()));
        }
    }

    // Elimina al jugador del catálogo SIN borrar productos: solo se vacía su llave en la tabla productos
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            String imagen = catalogoService.eliminarJugador(id);
            storage.eliminarPorUrl(imagen);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, String>> error(String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }

    private ResponseEntity<Map<String, String>> sinLlave() {
        return ResponseEntity.status(500).body(Map.of("error",
                "Falta configurar la llave de Supabase en el backend (application-secrets.properties) para subir imágenes"));
    }
}
