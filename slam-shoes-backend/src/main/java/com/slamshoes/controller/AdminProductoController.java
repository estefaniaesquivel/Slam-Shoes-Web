package com.slamshoes.controller;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.dto.ColorTallasDTO;
import com.slamshoes.dto.ProductoNuevoDTO;
import com.slamshoes.dto.TallaStockDTO;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.CategoriaRepository;
import com.slamshoes.repository.ColeccionRepository;
import com.slamshoes.repository.JugadorRepository;
import com.slamshoes.repository.MarcaRepository;
import com.slamshoes.service.ProductoAdminService;
import com.slamshoes.service.SupabaseStorageService;

@RestController
@RequestMapping("/api/admin/productos")
@CrossOrigin(origins = "*")
public class AdminProductoController {

    private static final List<String> COLORES = List.of("Naranja", "Amarillo", "Verde", "Azul", "Rojo", "Blanco",
            "Gris", "Negro", "Café", "Morado", "Rosa", "Multicolor");

    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ColeccionRepository coleccionRepository;
    @Autowired private JugadorRepository jugadorRepository;
    @Autowired private ProductoAdminService productoAdminService;
    @Autowired private SupabaseStorageService storage;

    // "Añadir producto al catálogo": datos del producto (JSON) + imágenes en orden (archivos .webp)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(@RequestPart("producto") ProductoNuevoDTO datos,
                                   @RequestPart("imagenes") List<MultipartFile> imagenes) {

        // ---------- Validaciones ----------
        if (datos.nombre() == null || datos.nombre().isBlank()) return error("El nombre es obligatorio");
        if (datos.precio() == null || datos.precio() <= 0) return error("El precio debe ser mayor a 0");
        if (datos.idCategoria() == null || !categoriaRepository.existsById(datos.idCategoria())) {
            return error("Selecciona una categoría válida");
        }
        var marca = datos.idMarca() == null ? null : marcaRepository.findById(datos.idMarca()).orElse(null);
        if (marca == null) return error("Selecciona una marca válida");

        // Colección y jugador son opcionales, pero si se escriben deben existir
        Long idColeccion = null;
        if (datos.coleccion() != null && !datos.coleccion().isBlank()) {
            var c = coleccionRepository.findByNombreIgnoreCase(datos.coleccion().trim());
            if (c.isEmpty()) return error("La colección \"" + datos.coleccion() + "\" no existe");
            idColeccion = c.get().getIdColeccion();
        }
        Long idJugador = null;
        if (datos.jugador() != null && !datos.jugador().isBlank()) {
            var j = jugadorRepository.findByNombreIgnoreCase(datos.jugador().trim());
            if (j.isEmpty()) return error("El jugador \"" + datos.jugador() + "\" no existe");
            idJugador = j.get().getIdJugador();
        }

        if (datos.colores() == null || datos.colores().isEmpty()) return error("Selecciona al menos un color");
        for (ColorTallasDTO c : datos.colores()) {
            if (c.color() == null || !COLORES.contains(c.color())) return error("Color no válido: " + c.color());
            if (c.tallas() == null || c.tallas().isEmpty()) return error("El color " + c.color() + " no tiene tallas");
            for (TallaStockDTO t : c.tallas()) {
                if (t.talla() == null || t.talla().isBlank() || t.stock() == null || t.stock() < 0) {
                    return error("Talla o stock no válidos en el color " + c.color());
                }
            }
        }

        if (imagenes == null || imagenes.isEmpty()) return error("Sube al menos una imagen");
        for (MultipartFile f : imagenes) {
            String tipo = f.getContentType();
            String nombreArchivo = f.getOriginalFilename() == null ? "" : f.getOriginalFilename().toLowerCase();
            if (f.isEmpty() || !("image/webp".equals(tipo) || nombreArchivo.endsWith(".webp"))) {
                return error("Todas las imágenes deben estar en formato .webp");
            }
        }
        if (!storage.estaConfigurado()) {
            return ResponseEntity.status(500).body(Map.of("error",
                    "Falta configurar SUPABASE_SERVICE_KEY en el backend para subir imágenes"));
        }

        // ---------- Subida a Supabase Storage: carpeta con el nombre del tenis, archivos 1.webp, 2.webp... ----------
        String carpeta = aCarpeta(datos.nombre());
        List<String> rutas = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        try {
            for (int i = 0; i < imagenes.size(); i++) {
                String archivo = (i + 1) + ".webp"; // el número de archivo es el orden asignado
                urls.add(storage.subir(carpeta, archivo, imagenes.get(i).getBytes(), "image/webp"));
                rutas.add(carpeta + "/" + archivo);
            }
        } catch (IOException | InterruptedException e) {
            storage.eliminar(rutas);
            return ResponseEntity.status(502).body(Map.of("error",
                    "No se pudieron subir las imágenes a Supabase (¿ya existe un producto con ese nombre?): " + e.getMessage()));
        }

        // ---------- Guardado en la base de datos ----------
        try {
            Producto creado = productoAdminService.guardar(datos, marca.getNombre(), idColeccion, idJugador, urls);
            return ResponseEntity.status(201).body(Map.of(
                    "idProducto", creado.getIdProducto(),
                    "nombre", creado.getNombre(),
                    "carpeta", carpeta));
        } catch (RuntimeException e) {
            storage.eliminar(rutas); // no dejar imágenes huérfanas
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo guardar el producto: " + e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, String>> error(String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }

    // "Nike Kyrie 8 N7" -> "nike-kyrie-8-n7" (sin acentos ni caracteres que Storage no acepta)
    private String aCarpeta(String nombre) {
        String s = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "producto" : s;
    }
}
