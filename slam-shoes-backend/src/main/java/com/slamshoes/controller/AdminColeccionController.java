package com.slamshoes.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.dto.ColeccionAdminDTO;
import com.slamshoes.dto.ColeccionAdminDetalleDTO;
import com.slamshoes.dto.ProductoMiniDTO;
import com.slamshoes.model.Coleccion;
import com.slamshoes.repository.ColeccionRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.service.AdminCatalogoService;
import com.slamshoes.service.SupabaseStorageService;

// Agregar y editar colecciones (portada de 2000x2000 px en Storage, carpeta "colecciones")
@RestController
@RequestMapping("/api/admin/colecciones")
@CrossOrigin(origins = "*")
public class AdminColeccionController {

    private static final String CARPETA = "colecciones";

    @Autowired private ColeccionRepository coleccionRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private AdminCatalogoService catalogoService;
    @Autowired private SupabaseStorageService storage;

    // Todas las colecciones (también las inactivas) para el panel de edición
    @GetMapping
    public List<Coleccion> listar() {
        return coleccionRepository.findAll(Sort.by("nombre").ascending());
    }

    // Datos guardados + productos que pertenecen a la colección (para rellenar el formulario de edición)
    @GetMapping("/{id}")
    public ResponseEntity<ColeccionAdminDetalleDTO> detalle(@PathVariable Long id) {
        return coleccionRepository.findById(id).map(c -> {
            List<ProductoMiniDTO> productos = productoRepository.findByIdColeccion(id).stream()
                    .map(p -> new ProductoMiniDTO(p.getIdProducto(), p.getNombre(), p.getImagenUrl()))
                    .sorted((a, b) -> Long.compare(a.idProducto(), b.idProducto()))
                    .toList();
            return ResponseEntity.ok(new ColeccionAdminDetalleDTO(
                    c.getIdColeccion(), c.getNombre(), c.getDescripcion(), c.getImagenUrl(), productos));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(@RequestPart("coleccion") ColeccionAdminDTO datos,
                                   @RequestPart("imagen") MultipartFile imagen) {
        if (datos.nombre() == null || datos.nombre().isBlank()) return error("El nombre de la colección es obligatorio");
        if (!SupabaseStorageService.esWebp(imagen)) return error("Sube la portada de la colección en formato .webp");
        if (!storage.estaConfigurado()) return sinLlave();

        String url;
        try {
            url = storage.subirWebp(CARPETA, datos.nombre(), imagen);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo subir la imagen a Supabase: " + e.getMessage()));
        }
        try {
            Coleccion c = catalogoService.guardarColeccion(null, datos.nombre(), datos.descripcion(), datos.idsProductos(), url);
            return ResponseEntity.status(201).body(Map.of("idColeccion", c.getIdColeccion(), "nombre", c.getNombre()));
        } catch (IllegalArgumentException e) {
            storage.eliminarPorUrl(url);
            return error(e.getMessage());
        } catch (RuntimeException e) {
            storage.eliminarPorUrl(url);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo guardar la colección: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @RequestPart("coleccion") ColeccionAdminDTO datos,
                                        @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        Coleccion actual = coleccionRepository.findById(id).orElse(null);
        if (actual == null) return ResponseEntity.notFound().build();
        if (datos.nombre() == null || datos.nombre().isBlank()) return error("El nombre de la colección es obligatorio");

        boolean hayImagenNueva = imagen != null && !imagen.isEmpty();
        if (hayImagenNueva && !SupabaseStorageService.esWebp(imagen)) return error("La portada debe estar en formato .webp");
        if (hayImagenNueva && !storage.estaConfigurado()) return sinLlave();

        String urlAnterior = actual.getImagenUrl();
        String urlNueva = null;
        try {
            if (hayImagenNueva) urlNueva = storage.subirWebp(CARPETA, datos.nombre(), imagen);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo subir la imagen a Supabase: " + e.getMessage()));
        }
        try {
            Coleccion c = catalogoService.guardarColeccion(id, datos.nombre(), datos.descripcion(), datos.idsProductos(), urlNueva);
            if (urlNueva != null) storage.eliminarPorUrl(urlAnterior);
            return ResponseEntity.ok(Map.of("idColeccion", c.getIdColeccion(), "nombre", c.getNombre()));
        } catch (IllegalArgumentException e) {
            if (urlNueva != null) storage.eliminarPorUrl(urlNueva);
            return error(e.getMessage());
        } catch (RuntimeException e) {
            if (urlNueva != null) storage.eliminarPorUrl(urlNueva);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo actualizar la colección: " + e.getMessage()));
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
