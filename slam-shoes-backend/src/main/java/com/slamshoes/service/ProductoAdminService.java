package com.slamshoes.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.dto.ColorTallasDTO;
import com.slamshoes.dto.ImagenAdminDTO;
import com.slamshoes.dto.ProductoAdminDetalleDTO;
import com.slamshoes.dto.ProductoBaseDTO;
import com.slamshoes.dto.ProductoEdicionDTO;
import com.slamshoes.dto.ProductoNuevoDTO;
import com.slamshoes.dto.TallaStockDTO;
import com.slamshoes.exception.ConflictoEliminacionException;
import com.slamshoes.exception.RecursoNoEncontradoException;
import com.slamshoes.exception.StorageException;
import com.slamshoes.exception.StorageNoConfiguradoException;
import com.slamshoes.exception.ValidacionException;
import com.slamshoes.model.Producto;
import com.slamshoes.model.ProductoImagen;
import com.slamshoes.model.ProductoTalla;
import com.slamshoes.repository.CategoriaRepository;
import com.slamshoes.repository.ColeccionRepository;
import com.slamshoes.repository.JugadorRepository;
import com.slamshoes.repository.MarcaRepository;
import com.slamshoes.repository.PedidoDetalleRepository;
import com.slamshoes.repository.ProductoImagenRepository;
import com.slamshoes.repository.ProductoRepository;
import com.slamshoes.repository.ProductoTallaRepository;

/**
 * Servicio transaccional encargado de la creación, edición, detalle y baja de productos administrativos.
 */
@Service
public class ProductoAdminService implements IProductoAdminService {

    /** Etiqueta utilizada en el formulario cuando una variante no especifica color. */
    public static final String SIN_COLOR = "Sin color";

    private static final List<String> COLORES_PERMITIDOS = List.of(
            "Naranja", "Amarillo", "Verde", "Azul", "Rojo", "Blanco",
            "Gris", "Negro", "Café", "Morado", "Rosa", "Multicolor");

    private final ProductoRepository productoRepository;
    private final ProductoTallaRepository productoTallaRepository;
    private final ProductoImagenRepository productoImagenRepository;
    private final PedidoDetalleRepository pedidoDetalleRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ColeccionRepository coleccionRepository;
    private final JugadorRepository jugadorRepository;
    private final IStorageService storageService;

    private record RelacionesResueltas(String nombreMarca, Long idColeccion, Long idJugador) {}

    /**
     * Inicializa el servicio con los repositorios de catálogo/pedidos y el servicio de almacenamiento.
     *
     * @param productoRepository repositorio de productos
     * @param productoTallaRepository repositorio de variantes talla/color
     * @param productoImagenRepository repositorio de galería de imágenes
     * @param pedidoDetalleRepository repositorio de detalles de pedido para validar historial
     * @param categoriaRepository repositorio de categorías
     * @param marcaRepository repositorio de marcas
     * @param coleccionRepository repositorio de colecciones
     * @param jugadorRepository repositorio de jugadores
     * @param storageService servicio de almacenamiento de imágenes
     */
    @Autowired
    public ProductoAdminService(
            ProductoRepository productoRepository,
            ProductoTallaRepository productoTallaRepository,
            ProductoImagenRepository productoImagenRepository,
            PedidoDetalleRepository pedidoDetalleRepository,
            CategoriaRepository categoriaRepository,
            MarcaRepository marcaRepository,
            ColeccionRepository coleccionRepository,
            JugadorRepository jugadorRepository,
            IStorageService storageService) {
        this.productoRepository = productoRepository;
        this.productoTallaRepository = productoTallaRepository;
        this.productoImagenRepository = productoImagenRepository;
        this.pedidoDetalleRepository = pedidoDetalleRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.coleccionRepository = coleccionRepository;
        this.jugadorRepository = jugadorRepository;
        this.storageService = storageService;
    }

    @Override
    public ProductoAdminDetalleDTO obtenerDetalle(Long id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));

        String coleccion = p.getIdColeccion() == null ? "" : coleccionRepository.findById(p.getIdColeccion())
                .map(c -> c.getNombre()).orElse("");
        String jugador = p.getIdJugador() == null ? "" : jugadorRepository.findById(p.getIdJugador())
                .map(j -> j.getNombre()).orElse("");

        Map<String, List<TallaStockDTO>> porColor = new LinkedHashMap<>();
        for (ProductoTalla v : productoTallaRepository.findByIdProducto(id)) {
            String color = v.getColor() == null ? SIN_COLOR : v.getColor();
            porColor.computeIfAbsent(color, k -> new ArrayList<>())
                    .add(new TallaStockDTO(v.getTalla(), v.getStock()));
        }
        List<ColorTallasDTO> colores = porColor.entrySet().stream()
                .map(e -> new ColorTallasDTO(e.getKey(), e.getValue())).toList();

        List<ImagenAdminDTO> imagenes = productoImagenRepository.findByIdProducto(id).stream()
                .sorted(Comparator.comparing(ProductoImagen::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(i -> new ImagenAdminDTO(i.getUrlImagen(), i.getOrden()))
                .toList();

        return new ProductoAdminDetalleDTO(
                p.getIdProducto(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getIdCategoria(),
                p.getIdMarca(), coleccion, jugador, p.getActivo(), p.getModelo(),
                Boolean.TRUE.equals(p.getEsTendencia()), colores, imagenes);
    }

    @Override
    @Transactional
    public Map<String, Object> crearCompleto(ProductoNuevoDTO datos, List<MultipartFile> imagenes) {
        RelacionesResueltas r = validarComunes(datos, List.of());
        if (imagenes == null || imagenes.isEmpty()) {
            throw new ValidacionException("Sube al menos una imagen");
        }
        validarWebp(imagenes);
        if (!storageService.estaConfigurado()) {
            throw new StorageNoConfiguradoException();
        }

        String carpeta = SupabaseStorageService.slug(datos.nombre());
        List<String> rutas = new ArrayList<>();
        List<String> urls = subirImagenesNuevas(carpeta, imagenes, rutas);

        try {
            Producto creado = guardar(datos, r.nombreMarca(), r.idColeccion(), r.idJugador(), urls);
            return Map.of("idProducto", creado.getIdProducto(), "nombre", creado.getNombre(), "carpeta", carpeta);
        } catch (RuntimeException e) {
            storageService.eliminar(rutas);
            throw e;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> editarCompleto(Long id, ProductoEdicionDTO datos, List<MultipartFile> nuevas) {
        if (!productoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("El producto no existe");
        }
        List<MultipartFile> archivos = nuevas == null ? List.of() : nuevas;

        List<String> urlsActuales = productoImagenRepository.findByIdProducto(id).stream()
                .map(ProductoImagen::getUrlImagen).toList();
        List<String> coloresActuales = productoTallaRepository.findByIdProducto(id).stream()
                .map(v -> v.getColor() == null ? SIN_COLOR : v.getColor()).distinct().toList();

        RelacionesResueltas r = validarComunes(datos, coloresActuales);
        validarGaleriaEdicion(datos.imagenes(), archivos, urlsActuales);
        if (!archivos.isEmpty() && !storageService.estaConfigurado()) {
            throw new StorageNoConfiguradoException();
        }

        String carpeta = SupabaseStorageService.slug(datos.nombre());
        List<String> rutasNuevas = new ArrayList<>();
        List<String> urlsFinales = resolverUrlsEdicion(carpeta, datos.imagenes(), archivos, rutasNuevas);

        try {
            Producto actualizado = actualizar(id, datos, r.nombreMarca(), r.idColeccion(), r.idJugador(), urlsFinales);
            limpiarImagenesQuitadas(urlsActuales, urlsFinales);
            return Map.of("idProducto", actualizado.getIdProducto(), "nombre", actualizado.getNombre());
        } catch (RuntimeException e) {
            storageService.eliminar(rutasNuevas);
            throw e;
        }
    }

    /**
     * Persiste un producto nuevo junto con sus variantes y galería de imágenes en una transacción.
     *
     * @param d datos del nuevo producto
     * @param nombreMarca nombre resuelto de la marca
     * @param idColeccion identificador de colección opcional
     * @param idJugador identificador de jugador opcional
     * @param urlsImagenes URLs públicas de la galería en orden
     * @return entidad {@link Producto} guardada
     */
    @Transactional
    public Producto guardar(ProductoNuevoDTO d, String nombreMarca, Long idColeccion, Long idJugador, List<String> urlsImagenes) {
        Producto p = new Producto();
        aplicarDatosComunes(p, d, nombreMarca, idColeccion, idJugador, urlsImagenes.get(0));
        p = productoRepository.save(p);

        for (ColorTallasDTO c : d.colores()) {
            for (TallaStockDTO t : c.tallas()) {
                ProductoTalla v = new ProductoTalla();
                v.setIdProducto(p.getIdProducto());
                v.setColor(colorBD(c.color()));
                v.setTalla(t.talla());
                v.setStock(t.stock() == null ? 0 : t.stock());
                productoTallaRepository.save(v);
            }
        }
        guardarImagenes(p.getIdProducto(), urlsImagenes);
        return p;
    }

    /**
     * Actualiza un producto existente, sincroniza sus variantes y reemplaza su galería de imágenes.
     *
     * @param idProducto identificador del producto
     * @param d datos actualizados del producto
     * @param nombreMarca nombre resuelto de la marca
     * @param idColeccion identificador de colección opcional
     * @param idJugador identificador de jugador opcional
     * @param urlsImagenes URLs finales de la galería en orden
     * @return entidad {@link Producto} actualizada
     */
    @Transactional
    public Producto actualizar(Long idProducto, ProductoEdicionDTO d, String nombreMarca, Long idColeccion,
                               Long idJugador, List<String> urlsImagenes) {
        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));

        aplicarDatosComunes(p, d, nombreMarca, idColeccion, idJugador, urlsImagenes.get(0));
        p = productoRepository.save(p);

        sincronizarVariantes(idProducto, d.colores());
        productoImagenRepository.deleteByIdProducto(idProducto);
        guardarImagenes(idProducto, urlsImagenes);
        return p;
    }

    @Override
    @Transactional
    public void eliminar(Long idProducto) {
        if (!productoRepository.existsById(idProducto)) {
            throw new RecursoNoEncontradoException("El producto no existe");
        }
        for (ProductoTalla v : productoTallaRepository.findByIdProducto(idProducto)) {
            if (pedidoDetalleRepository.existsByIdVariante(v.getIdVariante())) {
                throw new ConflictoEliminacionException(
                        "Este producto ya aparece en pedidos y no se puede eliminar. Márcalo como inactivo desde su edición.");
            }
        }
        List<String> rutas = new ArrayList<>();
        for (ProductoImagen i : productoImagenRepository.findByIdProducto(idProducto)) {
            String ruta = storageService.rutaDesdeUrl(i.getUrlImagen());
            if (ruta != null) rutas.add(ruta);
        }
        productoRepository.deleteById(idProducto);
        if (storageService.estaConfigurado()) {
            storageService.eliminar(rutas);
        }
    }

    private void aplicarDatosComunes(Producto p, ProductoBaseDTO d, String nombreMarca,
                                     Long idColeccion, Long idJugador, String urlPortada) {
        p.setNombre(d.nombre().trim());
        p.setDescripcion(d.descripcion() == null || d.descripcion().isBlank() ? null : d.descripcion().trim());
        p.setPrecio(d.precio());
        p.setIdCategoria(d.idCategoria());
        p.setIdMarca(d.idMarca());
        p.setMarca(nombreMarca);
        p.setIdColeccion(idColeccion);
        p.setIdJugador(idJugador);
        p.setActivo(d.activo() == null ? Boolean.TRUE : d.activo());
        p.setModelo(d.modelo() == null || d.modelo().isBlank() ? null : d.modelo().trim());
        p.setEsTendencia(Boolean.TRUE.equals(d.esTendencia()));
        p.setImagenUrl(urlPortada);
    }

    private RelacionesResueltas validarComunes(ProductoBaseDTO d, List<String> coloresPermitidosExtra) {
        if (d.nombre() == null || d.nombre().isBlank()) throw new ValidacionException("El nombre es obligatorio");
        if (d.precio() == null || d.precio() <= 0) throw new ValidacionException("El precio debe ser mayor a 0");
        if (d.idCategoria() == null || !categoriaRepository.existsById(d.idCategoria())) {
            throw new ValidacionException("Selecciona una categoría válida");
        }
        var marca = d.idMarca() == null ? null : marcaRepository.findById(d.idMarca()).orElse(null);
        if (marca == null) throw new ValidacionException("Selecciona una marca válida");

        Long idColeccion = resolverColeccion(d.coleccion());
        Long idJugador = resolverJugador(d.jugador());
        validarColoresYTallas(d.colores(), coloresPermitidosExtra);
        return new RelacionesResueltas(marca.getNombre(), idColeccion, idJugador);
    }

    private Long resolverColeccion(String coleccion) {
        if (coleccion == null || coleccion.isBlank()) return null;
        return coleccionRepository.findByNombreIgnoreCase(coleccion.trim())
                .orElseThrow(() -> new ValidacionException("La colección \"" + coleccion + "\" no existe"))
                .getIdColeccion();
    }

    private Long resolverJugador(String jugador) {
        if (jugador == null || jugador.isBlank()) return null;
        return jugadorRepository.findByNombreIgnoreCase(jugador.trim())
                .orElseThrow(() -> new ValidacionException("El jugador \"" + jugador + "\" no existe"))
                .getIdJugador();
    }

    private void validarColoresYTallas(List<ColorTallasDTO> colores, List<String> coloresPermitidosExtra) {
        if (colores == null || colores.isEmpty()) throw new ValidacionException("Selecciona al menos un color");
        for (ColorTallasDTO c : colores) {
            boolean valido = c.color() != null
                    && (COLORES_PERMITIDOS.contains(c.color()) || coloresPermitidosExtra.contains(c.color()));
            if (!valido) throw new ValidacionException("Color no válido: " + c.color());
            if (c.tallas() == null || c.tallas().isEmpty()) {
                throw new ValidacionException("El color " + c.color() + " no tiene tallas");
            }
            for (TallaStockDTO t : c.tallas()) {
                if (t.talla() == null || t.talla().isBlank() || t.stock() == null || t.stock() < 0) {
                    throw new ValidacionException("Talla o stock no válidos en el color " + c.color());
                }
            }
        }
    }

    private void validarGaleriaEdicion(List<ImagenAdminDTO> galeria, List<MultipartFile> archivos, List<String> urlsActuales) {
        if (galeria == null || galeria.isEmpty()) {
            throw new ValidacionException("El producto necesita al menos una imagen");
        }
        long cantidadNuevas = galeria.stream().filter(i -> i.url() == null).count();
        if (cantidadNuevas != archivos.size()) {
            throw new ValidacionException("Las imágenes nuevas no coinciden con los recuadros");
        }
        for (ImagenAdminDTO i : galeria) {
            if (i.url() != null && !urlsActuales.contains(i.url())) {
                throw new ValidacionException("Una imagen existente no pertenece a este producto");
            }
        }
        validarWebp(archivos);
    }

    private void validarWebp(List<MultipartFile> archivos) {
        for (MultipartFile f : archivos) {
            if (!SupabaseStorageService.esWebp(f)) {
                throw new ValidacionException("Todas las imágenes deben estar en formato .webp");
            }
        }
    }

    private List<String> subirImagenesNuevas(String carpeta, List<MultipartFile> imagenes, List<String> rutas) {
        List<String> urls = new ArrayList<>();
        try {
            for (int i = 0; i < imagenes.size(); i++) {
                String archivo = (i + 1) + ".webp";
                urls.add(storageService.subir(carpeta, archivo, imagenes.get(i).getBytes(), "image/webp"));
                rutas.add(carpeta + "/" + archivo);
            }
            return urls;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            storageService.eliminar(rutas);
            throw new StorageException(
                    "No se pudieron subir las imágenes a Supabase (¿ya existe un producto con ese nombre?): " + e.getMessage(), e);
        }
    }

    private List<String> resolverUrlsEdicion(String carpeta, List<ImagenAdminDTO> galeria,
                                             List<MultipartFile> archivos, List<String> rutasNuevas) {
        List<String> urlsFinales = new ArrayList<>();
        try {
            int siguiente = 0;
            for (int pos = 0; pos < galeria.size(); pos++) {
                String urlExistente = galeria.get(pos).url();
                if (urlExistente != null) {
                    urlsFinales.add(urlExistente);
                } else {
                    String archivo = (pos + 1) + "-" + System.currentTimeMillis() + ".webp";
                    urlsFinales.add(storageService.subir(carpeta, archivo, archivos.get(siguiente++).getBytes(), "image/webp"));
                    rutasNuevas.add(carpeta + "/" + archivo);
                }
            }
            return urlsFinales;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            storageService.eliminar(rutasNuevas);
            throw new StorageException("No se pudieron subir las imágenes a Supabase: " + e.getMessage(), e);
        }
    }

    private void limpiarImagenesQuitadas(List<String> urlsActuales, List<String> urlsFinales) {
        if (!storageService.estaConfigurado()) return;
        List<String> rutasQuitadas = new ArrayList<>();
        for (String url : urlsActuales) {
            if (!urlsFinales.contains(url)) {
                String ruta = storageService.rutaDesdeUrl(url);
                if (ruta != null) rutasQuitadas.add(ruta);
            }
        }
        storageService.eliminar(rutasQuitadas);
    }

    private void guardarImagenes(Long idProducto, List<String> urls) {
        for (int i = 0; i < urls.size(); i++) {
            ProductoImagen img = new ProductoImagen();
            img.setIdProducto(idProducto);
            img.setUrlImagen(urls.get(i));
            img.setOrden(i + 1);
            productoImagenRepository.save(img);
        }
    }

    private void sincronizarVariantes(Long idProducto, List<ColorTallasDTO> colores) {
        Map<String, ProductoTalla> actuales = new HashMap<>();
        for (ProductoTalla v : productoTallaRepository.findByIdProducto(idProducto)) {
            actuales.put(clave(v.getColor(), v.getTalla()), v);
        }
        Set<String> deseadas = actualizarVariantesExistentesYNuevas(idProducto, colores, actuales);
        limpiarVariantesObsoletas(actuales, deseadas);
    }

    private Set<String> actualizarVariantesExistentesYNuevas(Long idProducto, List<ColorTallasDTO> colores,
                                                              Map<String, ProductoTalla> actuales) {
        Set<String> deseadas = new HashSet<>();
        for (ColorTallasDTO c : colores) {
            for (TallaStockDTO t : c.tallas()) {
                String k = clave(colorBD(c.color()), t.talla());
                deseadas.add(k);
                ProductoTalla v = actuales.get(k);
                if (v == null) {
                    v = new ProductoTalla();
                    v.setIdProducto(idProducto);
                    v.setColor(colorBD(c.color()));
                    v.setTalla(t.talla());
                }
                v.setStock(t.stock() == null ? 0 : t.stock());
                productoTallaRepository.save(v);
            }
        }
        return deseadas;
    }

    private void limpiarVariantesObsoletas(Map<String, ProductoTalla> actuales, Set<String> deseadas) {
        for (Map.Entry<String, ProductoTalla> e : actuales.entrySet()) {
            if (deseadas.contains(e.getKey())) continue;
            ProductoTalla v = e.getValue();
            if (pedidoDetalleRepository.existsByIdVariante(v.getIdVariante())) {
                v.setStock(0);
                productoTallaRepository.save(v);
            } else {
                productoTallaRepository.delete(v);
            }
        }
    }

    private static String colorBD(String color) {
        return SIN_COLOR.equals(color) ? null : color;
    }

    private static String clave(String color, String talla) {
        return (color == null ? SIN_COLOR : color).toLowerCase() + "|" + talla;
    }
}
