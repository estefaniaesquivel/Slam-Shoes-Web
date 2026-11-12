package com.slamshoes.controller;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.slamshoes.dto.ColorTallasDTO;
import com.slamshoes.dto.ImagenAdminDTO;
import com.slamshoes.dto.ProductoAdminDetalleDTO;
import com.slamshoes.dto.ProductoEdicionDTO;
import com.slamshoes.dto.ProductoNuevoDTO;
import com.slamshoes.dto.TallaStockDTO;
import com.slamshoes.model.Producto;
import com.slamshoes.model.ProductoImagen;
import com.slamshoes.model.ProductoTalla;
import com.slamshoes.repository.CategoriaRepository;
import com.slamshoes.repository.ColeccionRepository;
import com.slamshoes.repository.JugadorRepository;
import com.slamshoes.repository.MarcaRepository;
import com.slamshoes.repository.ProductoImagenRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.ProductoTallaRepository;
import com.slamshoes.service.ProductoAdminService;
import com.slamshoes.service.SupabaseStorageService;

@RestController
@RequestMapping("/api/admin/productos")
@CrossOrigin(origins = "*")
public class AdminProductoController {

    private static final List<String> COLORES = List.of("Naranja", "Amarillo", "Verde", "Azul", "Rojo", "Blanco",
            "Gris", "Negro", "Café", "Morado", "Rosa", "Multicolor");

    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoTallaRepository productoTallaRepository;
    @Autowired private ProductoImagenRepository productoImagenRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ColeccionRepository coleccionRepository;
    @Autowired private JugadorRepository jugadorRepository;
    @Autowired private ProductoAdminService productoAdminService;
    @Autowired private SupabaseStorageService storage;

    // Datos ya validados: nombre de la marca y ids de colección y jugador (null si no aplica)
    private record Resueltos(String nombreMarca, Long idColeccion, Long idJugador) {}

    // ---------------------------------------------------------------- Detalle (para rellenar la edición)
    @GetMapping("/{id}")
    public ResponseEntity<ProductoAdminDetalleDTO> detalle(@PathVariable Long id) {
        Producto p = productoRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();

        String coleccion = p.getIdColeccion() == null ? "" : coleccionRepository.findById(p.getIdColeccion())
                .map(c -> c.getNombre()).orElse("");
        String jugador = p.getIdJugador() == null ? "" : jugadorRepository.findById(p.getIdJugador())
                .map(j -> j.getNombre()).orElse("");

        // Variantes agrupadas por color
        Map<String, List<TallaStockDTO>> porColor = new LinkedHashMap<>();
        for (ProductoTalla v : productoTallaRepository.findByIdProducto(id)) {
            String color = v.getColor() == null ? ProductoAdminService.SIN_COLOR : v.getColor();
            porColor.computeIfAbsent(color, k -> new ArrayList<>())
                    .add(new TallaStockDTO(v.getTalla(), v.getStock()));
        }
        List<ColorTallasDTO> colores = porColor.entrySet().stream()
                .map(e -> new ColorTallasDTO(e.getKey(), e.getValue())).toList();

        List<ImagenAdminDTO> imagenes = productoImagenRepository.findByIdProducto(id).stream()
                .sorted(Comparator.comparing(ProductoImagen::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(i -> new ImagenAdminDTO(i.getUrlImagen(), i.getOrden()))
                .toList();

        return ResponseEntity.ok(new ProductoAdminDetalleDTO(
                p.getIdProducto(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getIdCategoria(),
                p.getIdMarca(), coleccion, jugador, p.getActivo(), p.getModelo(),
                Boolean.TRUE.equals(p.getEsTendencia()), colores, imagenes));
    }

    // ---------------------------------------------------------------- Crear
    // "Añadir producto al catálogo": datos del producto (JSON) + imágenes en orden (archivos .webp)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(@RequestPart("producto") ProductoNuevoDTO datos,
                                   @RequestPart("imagenes") List<MultipartFile> imagenes) {
        Resueltos r;
        try {
            r = validarComunes(datos.nombre(), datos.precio(), datos.idCategoria(), datos.idMarca(),
                    datos.coleccion(), datos.jugador(), datos.colores(), List.of());
            if (imagenes == null || imagenes.isEmpty()) return error("Sube al menos una imagen");
            validarWebp(imagenes);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        if (!storage.estaConfigurado()) return sinLlave();

        // Subida a Supabase Storage: carpeta con el nombre del tenis, archivos 1.webp, 2.webp...
        String carpeta = aCarpeta(datos.nombre());
        List<String> rutas = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        try {
            for (int i = 0; i < imagenes.size(); i++) {
                String archivo = (i + 1) + ".webp";
                urls.add(storage.subir(carpeta, archivo, imagenes.get(i).getBytes(), "image/webp"));
                rutas.add(carpeta + "/" + archivo);
            }
        } catch (IOException | InterruptedException e) {
            storage.eliminar(rutas);
            return ResponseEntity.status(502).body(Map.of("error",
                    "No se pudieron subir las imágenes a Supabase (¿ya existe un producto con ese nombre?): " + e.getMessage()));
        }

        try {
            Producto creado = productoAdminService.guardar(datos, r.nombreMarca(), r.idColeccion(), r.idJugador(), urls);
            return ResponseEntity.status(201).body(Map.of(
                    "idProducto", creado.getIdProducto(), "nombre", creado.getNombre(), "carpeta", carpeta));
        } catch (RuntimeException e) {
            storage.eliminar(rutas); // no dejar imágenes huérfanas
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo guardar el producto: " + e.getMessage()));
        }
    }

    // ---------------------------------------------------------------- Editar
    // "imagenes" del JSON define la galería final en orden: url = se conserva, null = archivo nuevo (en el mismo orden)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editar(@PathVariable Long id,
                                    @RequestPart("producto") ProductoEdicionDTO datos,
                                    @RequestPart(value = "imagenes", required = false) List<MultipartFile> nuevas) {
        if (!productoRepository.existsById(id)) return ResponseEntity.notFound().build();
        List<MultipartFile> archivos = nuevas == null ? List.of() : nuevas;

        List<String> urlsActuales = productoImagenRepository.findByIdProducto(id).stream()
                .map(ProductoImagen::getUrlImagen).toList();
        List<String> coloresActuales = productoTallaRepository.findByIdProducto(id).stream()
                .map(v -> v.getColor() == null ? ProductoAdminService.SIN_COLOR : v.getColor()).distinct().toList();

        Resueltos r;
        try {
            r = validarComunes(datos.nombre(), datos.precio(), datos.idCategoria(), datos.idMarca(),
                    datos.coleccion(), datos.jugador(), datos.colores(), coloresActuales);
            if (datos.imagenes() == null || datos.imagenes().isEmpty()) throw new IllegalArgumentException("El producto necesita al menos una imagen");
            long cantidadNuevas = datos.imagenes().stream().filter(i -> i.url() == null).count();
            if (cantidadNuevas != archivos.size()) throw new IllegalArgumentException("Las imágenes nuevas no coinciden con los recuadros");
            for (ImagenAdminDTO i : datos.imagenes()) {
                if (i.url() != null && !urlsActuales.contains(i.url())) throw new IllegalArgumentException("Una imagen existente no pertenece a este producto");
            }
            validarWebp(archivos);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
        if (!archivos.isEmpty() && !storage.estaConfigurado()) return sinLlave();

        // Sube solo las imágenes nuevas (nombre único para no chocar con las existentes)
        String carpeta = aCarpeta(datos.nombre());
        List<String> rutasNuevas = new ArrayList<>();
        List<String> urlsFinales = new ArrayList<>();
        try {
            int siguiente = 0;
            for (int pos = 0; pos < datos.imagenes().size(); pos++) {
                String urlExistente = datos.imagenes().get(pos).url();
                if (urlExistente != null) {
                    urlsFinales.add(urlExistente);
                } else {
                    String archivo = (pos + 1) + "-" + System.currentTimeMillis() + ".webp";
                    urlsFinales.add(storage.subir(carpeta, archivo, archivos.get(siguiente++).getBytes(), "image/webp"));
                    rutasNuevas.add(carpeta + "/" + archivo);
                }
            }
        } catch (IOException | InterruptedException e) {
            storage.eliminar(rutasNuevas);
            return ResponseEntity.status(502).body(Map.of("error", "No se pudieron subir las imágenes a Supabase: " + e.getMessage()));
        }

        try {
            Producto actualizado = productoAdminService.actualizar(id, datos, r.nombreMarca(), r.idColeccion(), r.idJugador(), urlsFinales);

            // Limpieza: imágenes que ya no forman parte de la galería
            List<String> rutasQuitadas = new ArrayList<>();
            for (String url : urlsActuales) {
                if (!urlsFinales.contains(url)) {
                    String ruta = rutaDesdeUrl(url);
                    if (ruta != null) rutasQuitadas.add(ruta);
                }
            }
            if (storage.estaConfigurado()) storage.eliminar(rutasQuitadas);

            return ResponseEntity.ok(Map.of("idProducto", actualizado.getIdProducto(), "nombre", actualizado.getNombre()));
        } catch (RuntimeException e) {
            storage.eliminar(rutasNuevas);
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo actualizar el producto: " + e.getMessage()));
        }
    }

    // ---------------------------------------------------------------- Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!productoRepository.existsById(id)) return ResponseEntity.notFound().build();

        List<String> rutas = new ArrayList<>();
        for (ProductoImagen i : productoImagenRepository.findByIdProducto(id)) {
            String ruta = rutaDesdeUrl(i.getUrlImagen());
            if (ruta != null) rutas.add(ruta);
        }

        try {
            productoAdminService.eliminar(id);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo eliminar el producto: " + e.getMessage()));
        }

        if (storage.estaConfigurado()) storage.eliminar(rutas); // borra también sus imágenes de Storage
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- Utilidades
    // Valida lo común de crear y editar. Lanza IllegalArgumentException con el mensaje para el administrador.
    private Resueltos validarComunes(String nombre, Double precio, Long idCategoria, Long idMarca, String coleccion,
                                     String jugador, List<ColorTallasDTO> colores, List<String> coloresPermitidosExtra) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (precio == null || precio <= 0) throw new IllegalArgumentException("El precio debe ser mayor a 0");
        if (idCategoria == null || !categoriaRepository.existsById(idCategoria)) {
            throw new IllegalArgumentException("Selecciona una categoría válida");
        }
        var marca = idMarca == null ? null : marcaRepository.findById(idMarca).orElse(null);
        if (marca == null) throw new IllegalArgumentException("Selecciona una marca válida");

        // Colección y jugador son opcionales, pero si se escriben deben existir
        Long idColeccion = null;
        if (coleccion != null && !coleccion.isBlank()) {
            var c = coleccionRepository.findByNombreIgnoreCase(coleccion.trim());
            if (c.isEmpty()) throw new IllegalArgumentException("La colección \"" + coleccion + "\" no existe");
            idColeccion = c.get().getIdColeccion();
        }
        Long idJugador = null;
        if (jugador != null && !jugador.isBlank()) {
            var j = jugadorRepository.findByNombreIgnoreCase(jugador.trim());
            if (j.isEmpty()) throw new IllegalArgumentException("El jugador \"" + jugador + "\" no existe");
            idJugador = j.get().getIdJugador();
        }

        if (colores == null || colores.isEmpty()) throw new IllegalArgumentException("Selecciona al menos un color");
        for (ColorTallasDTO c : colores) {
            boolean valido = c.color() != null && (COLORES.contains(c.color()) || coloresPermitidosExtra.contains(c.color()));
            if (!valido) throw new IllegalArgumentException("Color no válido: " + c.color());
            if (c.tallas() == null || c.tallas().isEmpty()) throw new IllegalArgumentException("El color " + c.color() + " no tiene tallas");
            for (TallaStockDTO t : c.tallas()) {
                if (t.talla() == null || t.talla().isBlank() || t.stock() == null || t.stock() < 0) {
                    throw new IllegalArgumentException("Talla o stock no válidos en el color " + c.color());
                }
            }
        }
        return new Resueltos(marca.getNombre(), idColeccion, idJugador);
    }

    private void validarWebp(List<MultipartFile> archivos) {
        for (MultipartFile f : archivos) {
            String tipo = f.getContentType();
            String nombreArchivo = f.getOriginalFilename() == null ? "" : f.getOriginalFilename().toLowerCase();
            if (f.isEmpty() || !("image/webp".equals(tipo) || nombreArchivo.endsWith(".webp"))) {
                throw new IllegalArgumentException("Todas las imágenes deben estar en formato .webp");
            }
        }
    }

    private ResponseEntity<Map<String, String>> sinLlave() {
        return ResponseEntity.status(500).body(Map.of("error",
                "Falta configurar la llave de Supabase en el backend (application-secrets.properties) para subir imágenes"));
    }

    private ResponseEntity<Map<String, String>> error(String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }

    // De una URL pública de Supabase obtiene la ruta "carpeta/archivo" dentro del bucket (null si no es de Storage)
    private String rutaDesdeUrl(String url) {
        if (url == null) return null;
        String prefijo = storage.urlPublica("");
        return url.startsWith(prefijo) ? url.substring(prefijo.length()) : null;
    }

    // "Nike Kyrie 8 N7" -> "nike-kyrie-8-n7" (sin acentos ni caracteres que Storage no acepta)
    private String aCarpeta(String nombre) {
        String s = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "producto" : s;
    }
}
