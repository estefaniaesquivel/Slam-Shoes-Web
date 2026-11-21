package com.slamshoes.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.dto.BannerAdminDTO;
import com.slamshoes.dto.ColeccionAdminDTO;
import com.slamshoes.dto.ColeccionAdminDetalleDTO;
import com.slamshoes.dto.NombreImagenAdminDTO;
import com.slamshoes.dto.NovedadAdminDTO;
import com.slamshoes.dto.OfertaAdminDTO;
import com.slamshoes.dto.ProductoMiniDTO;
import com.slamshoes.exception.RecursoNoEncontradoException;
import com.slamshoes.exception.StorageException;
import com.slamshoes.exception.StorageNoConfiguradoException;
import com.slamshoes.exception.ValidacionException;
import com.slamshoes.model.Banner;
import com.slamshoes.model.Coleccion;
import com.slamshoes.model.Jugador;
import com.slamshoes.model.Marca;
import com.slamshoes.model.Novedad;
import com.slamshoes.model.Oferta;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.BannerRepository;
import com.slamshoes.repository.ColeccionRepository;
import com.slamshoes.repository.JugadorRepository;
import com.slamshoes.repository.MarcaRepository;
import com.slamshoes.repository.NovedadRepository;
import com.slamshoes.repository.OfertaRepository;
import com.slamshoes.repository.ProductoRepository;

/**
 * Servicio centralizado para operaciones administrativas de catálogo:
 * colecciones, banners, jugadores, marcas, novedades y ofertas.
 */
@Service
public class AdminCatalogoService {

    private final ColeccionRepository coleccionRepository;
    private final ProductoRepository productoRepository;
    private final JugadorRepository jugadorRepository;
    private final MarcaRepository marcaRepository;
    private final BannerRepository bannerRepository;
    private final NovedadRepository novedadRepository;
    private final OfertaRepository ofertaRepository;
    private final IStorageService storageService;

    /**
     * Inicializa el servicio con los repositorios de catálogo y almacenamiento.
     *
     * @param coleccionRepository repositorio de colecciones
     * @param productoRepository repositorio de productos
     * @param jugadorRepository repositorio de jugadores
     * @param marcaRepository repositorio de marcas
     * @param bannerRepository repositorio de banners
     * @param novedadRepository repositorio de novedades
     * @param ofertaRepository repositorio de ofertas
     * @param storageService servicio de almacenamiento de imágenes
     */
    @Autowired
    public AdminCatalogoService(
            ColeccionRepository coleccionRepository,
            ProductoRepository productoRepository,
            JugadorRepository jugadorRepository,
            MarcaRepository marcaRepository,
            BannerRepository bannerRepository,
            NovedadRepository novedadRepository,
            OfertaRepository ofertaRepository,
            IStorageService storageService) {
        this.coleccionRepository = coleccionRepository;
        this.productoRepository = productoRepository;
        this.jugadorRepository = jugadorRepository;
        this.marcaRepository = marcaRepository;
        this.bannerRepository = bannerRepository;
        this.novedadRepository = novedadRepository;
        this.ofertaRepository = ofertaRepository;
        this.storageService = storageService;
    }

    /**
     * Lista todas las colecciones ordenadas alfabéticamente por nombre.
     *
     * @return lista completa de colecciones
     */
    public List<Coleccion> listarColecciones() {
        return coleccionRepository.findAll(Sort.by("nombre").ascending());
    }

    /**
     * Obtiene el detalle de una colección junto con los productos asignados a ella.
     *
     * @param id identificador de la colección
     * @return DTO de detalle administrativo de colección
     */
    public ColeccionAdminDetalleDTO detalleColeccion(Long id) {
        Coleccion c = coleccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La colección no existe"));
        List<ProductoMiniDTO> productos = productoRepository.findByIdColeccion(id).stream()
                .map(p -> new ProductoMiniDTO(p.getIdProducto(), p.getNombre(), p.getImagenUrl()))
                .sorted((a, b) -> Long.compare(a.idProducto(), b.idProducto()))
                .toList();
        return new ColeccionAdminDetalleDTO(
                c.getIdColeccion(), c.getNombre(), c.getDescripcion(), c.getImagenUrl(), productos);
    }

    /**
     * Crea una colección subiendo su portada WebP y asignando sus productos.
     *
     * @param datos metadatos y lista de identificadores de productos
     * @param imagen archivo WebP de portada
     * @return colección persistida
     */
    @Transactional
    public Coleccion crearColeccion(ColeccionAdminDTO datos, MultipartFile imagen) {
        validarNombre(datos.nombre(), "El nombre de la colección es obligatorio");
        String url = subirImagenObligatoria("colecciones", datos.nombre(), imagen,
                "Sube la portada de la colección en formato .webp");
        try {
            return guardarColeccion(null, datos.nombre(), datos.descripcion(), datos.idsProductos(), url);
        } catch (RuntimeException e) {
            storageService.eliminarPorUrl(url);
            throw e;
        }
    }

    /**
     * Actualiza una colección existente y reemplaza su portada si se envía nueva imagen.
     *
     * @param id identificador de la colección
     * @param datos datos actualizados de la colección
     * @param imagen portada WebP opcional
     * @return colección actualizada
     */
    @Transactional
    public Coleccion actualizarColeccion(Long id, ColeccionAdminDTO datos, MultipartFile imagen) {
        Coleccion actual = coleccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La colección no existe"));
        validarNombre(datos.nombre(), "El nombre de la colección es obligatorio");
        String urlAnterior = actual.getImagenUrl();
        String urlNueva = subirImagenOpcional("colecciones", datos.nombre(), imagen,
                "La portada debe estar en formato .webp");
        try {
            Coleccion guardada = guardarColeccion(id, datos.nombre(), datos.descripcion(), datos.idsProductos(), urlNueva);
            if (urlNueva != null) storageService.eliminarPorUrl(urlAnterior);
            return guardada;
        } catch (RuntimeException e) {
            if (urlNueva != null) storageService.eliminarPorUrl(urlNueva);
            throw e;
        }
    }

    /**
     * Elimina una colección desvinculando sus productos sin eliminarlos.
     *
     * @param id identificador de la colección
     */
    @Transactional
    public void eliminarColeccion(Long id) {
        Coleccion c = coleccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La colección no existe"));
        for (Producto p : productoRepository.findByIdColeccion(id)) {
            p.setIdColeccion(null);
            productoRepository.save(p);
        }
        coleccionRepository.delete(c);
        storageService.eliminarPorUrl(c.getImagenUrl());
    }

    /**
     * Crea ({@code id == null}) o actualiza una colección dejando asignados exactamente los productos indicados.
     *
     * @param id identificador de la colección o {@code null} para crear
     * @param nombre nombre de la colección
     * @param descripcion descripción opcional
     * @param idsProductos lista de identificadores de productos asignados
     * @param imagenUrl URL de portada o {@code null} para conservar la existente
     * @return colección guardada
     */
    @Transactional
    public Coleccion guardarColeccion(Long id, String nombre, String descripcion, List<Long> idsProductos, String imagenUrl) {
        validarNombre(nombre, "El nombre de la colección es obligatorio");
        String nombreLimpio = nombre.trim();
        coleccionRepository.findByNombreIgnoreCase(nombreLimpio)
                .filter(otra -> !otra.getIdColeccion().equals(id))
                .ifPresent(otra -> { throw new ValidacionException("Ya existe una colección con ese nombre"); });

        Set<Long> ids = new HashSet<>(idsProductos == null ? List.of() : idsProductos);
        List<Producto> elegidos = productoRepository.findAllById(ids);
        if (elegidos.size() != ids.size()) {
            throw new ValidacionException("Alguno de los productos elegidos ya no existe");
        }

        Coleccion c;
        if (id == null) {
            c = new Coleccion();
            c.setActiva(true);
        } else {
            c = coleccionRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La colección no existe"));
        }
        c.setNombre(nombreLimpio);
        c.setDescripcion(descripcion == null ? "" : descripcion.trim());
        if (imagenUrl != null) c.setImagenUrl(imagenUrl);
        c = coleccionRepository.save(c);

        if (id != null) {
            for (Producto p : productoRepository.findByIdColeccion(id)) {
                if (!ids.contains(p.getIdProducto())) {
                    p.setIdColeccion(null);
                    productoRepository.save(p);
                }
            }
        }
        for (Producto p : elegidos) {
            p.setIdColeccion(c.getIdColeccion());
            productoRepository.save(p);
        }
        return c;
    }

    /**
     * Lista todos los banners ordenados por {@code orden} e {@code idBanner}.
     *
     * @return lista completa de banners
     */
    public List<Banner> listarBanners() {
        return bannerRepository.findAll(Sort.by("orden").ascending().and(Sort.by("idBanner").ascending()));
    }

    /**
     * Crea un banner subiendo su imagen WebP y calculando el orden siguiente si no se especifica.
     *
     * @param datos metadatos del banner
     * @param imagen imagen WebP del banner
     * @return banner guardado
     */
    @Transactional
    public Banner crearBanner(BannerAdminDTO datos, MultipartFile imagen) {
        validarNombre(datos.titulo(), "El título del banner es obligatorio");
        String url = subirImagenObligatoria("banners", datos.titulo(), imagen,
                "Sube la imagen del banner en formato .webp");
        try {
            Banner b = new Banner();
            aplicarBanner(b, datos);
            if (b.getOrden() == null) {
                b.setOrden(bannerRepository.findAll().stream()
                        .map(x -> x.getOrden() == null ? 0 : x.getOrden()).max(Integer::compare).orElse(0) + 1);
            }
            b.setImagenUrl(url);
            return bannerRepository.save(b);
        } catch (RuntimeException e) {
            storageService.eliminarPorUrl(url);
            throw e;
        }
    }

    /**
     * Actualiza un banner existente y reemplaza su imagen si se proporciona una nueva.
     *
     * @param id identificador del banner
     * @param datos metadatos actualizados
     * @param imagen nueva imagen WebP opcional
     * @return banner actualizado
     */
    @Transactional
    public Banner actualizarBanner(Long id, BannerAdminDTO datos, MultipartFile imagen) {
        Banner b = bannerRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El banner no existe"));
        validarNombre(datos.titulo(), "El título del banner es obligatorio");
        String urlAnterior = b.getImagenUrl();
        String urlNueva = subirImagenOpcional("banners", datos.titulo(), imagen,
                "La imagen debe estar en formato .webp");
        try {
            aplicarBanner(b, datos);
            if (urlNueva != null) b.setImagenUrl(urlNueva);
            Banner guardado = bannerRepository.save(b);
            if (urlNueva != null) storageService.eliminarPorUrl(urlAnterior);
            return guardado;
        } catch (RuntimeException e) {
            if (urlNueva != null) storageService.eliminarPorUrl(urlNueva);
            throw e;
        }
    }

    /**
     * Elimina un banner y borra su imagen asociada en el almacenamiento.
     *
     * @param id identificador del banner
     */
    @Transactional
    public void eliminarBanner(Long id) {
        Banner b = bannerRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El banner no existe"));
        bannerRepository.delete(b);
        storageService.eliminarPorUrl(b.getImagenUrl());
    }

    private void aplicarBanner(Banner b, BannerAdminDTO d) {
        b.setTitulo(d.titulo().trim());
        String enlace = d.enlaceDestino() == null ? "" : d.enlaceDestino().trim();
        b.setEnlaceDestino(enlace.isEmpty() ? null : enlace);
        if (d.orden() != null) b.setOrden(d.orden());
        b.setActivo(d.activo() == null || d.activo());
    }

    /**
     * Lista todos los jugadores ordenados alfabéticamente.
     *
     * @return lista completa de jugadores
     */
    public List<Jugador> listarJugadores() {
        return jugadorRepository.findAll(Sort.by("nombre").ascending());
    }

    /**
     * Crea un jugador subiendo su imagen WebP.
     *
     * @param datos nombre del jugador
     * @param imagen archivo WebP
     * @return jugador persistido
     */
    @Transactional
    public Jugador crearJugador(NombreImagenAdminDTO datos, MultipartFile imagen) {
        validarNombre(datos.nombre(), "El nombre es obligatorio");
        String url = subirImagenObligatoria("jugadores", datos.nombre(), imagen,
                "Sube la imagen en formato .webp");
        try {
            Jugador nuevo = new Jugador();
            nuevo.setNombre(datos.nombre().trim());
            nuevo.setImagenUrl(url);
            return jugadorRepository.save(nuevo);
        } catch (RuntimeException e) {
            storageService.eliminarPorUrl(url);
            throw e;
        }
    }

    /**
     * Actualiza un jugador existente y opcionalmente reemplaza su imagen.
     *
     * @param id identificador del jugador
     * @param datos datos actualizados
     * @param imagen nueva imagen WebP opcional
     * @return jugador actualizado
     */
    @Transactional
    public Jugador actualizarJugador(Long id, NombreImagenAdminDTO datos, MultipartFile imagen) {
        Jugador actual = jugadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El jugador no existe"));
        validarNombre(datos.nombre(), "El nombre es obligatorio");
        String urlAnterior = actual.getImagenUrl();
        String urlNueva = subirImagenOpcional("jugadores", datos.nombre(), imagen,
                "La imagen debe estar en formato .webp");
        try {
            actual.setNombre(datos.nombre().trim());
            if (urlNueva != null) actual.setImagenUrl(urlNueva);
            Jugador guardado = jugadorRepository.save(actual);
            if (urlNueva != null) storageService.eliminarPorUrl(urlAnterior);
            return guardado;
        } catch (RuntimeException e) {
            if (urlNueva != null) storageService.eliminarPorUrl(urlNueva);
            throw e;
        }
    }

    /**
     * Elimina un jugador sin borrar sus productos asociados y limpia su imagen.
     *
     * @param id identificador del jugador
     * @return URL de la imagen eliminada
     */
    @Transactional
    public String eliminarJugador(Long id) {
        var jugador = jugadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El jugador no existe"));
        for (Producto p : productoRepository.findByIdJugador(id)) {
            p.setIdJugador(null);
            productoRepository.save(p);
        }
        jugadorRepository.delete(jugador);
        storageService.eliminarPorUrl(jugador.getImagenUrl());
        return jugador.getImagenUrl();
    }

    /**
     * Lista todas las marcas ordenadas alfabéticamente.
     *
     * @return lista completa de marcas
     */
    public List<Marca> listarMarcas() {
        return marcaRepository.findAll(Sort.by("nombre").ascending());
    }

    /**
     * Crea una marca subiendo su logotipo WebP.
     *
     * @param datos nombre de la marca
     * @param imagen logotipo en formato WebP
     * @return marca persistida
     */
    @Transactional
    public Marca crearMarca(NombreImagenAdminDTO datos, MultipartFile imagen) {
        validarNombre(datos.nombre(), "El nombre es obligatorio");
        String url = subirImagenObligatoria("marcas", datos.nombre(), imagen,
                "Sube la imagen en formato .webp");
        try {
            Marca nueva = new Marca();
            nueva.setNombre(datos.nombre().trim());
            nueva.setLogoUrl(url);
            nueva.setActiva(true);
            return marcaRepository.save(nueva);
        } catch (RuntimeException e) {
            storageService.eliminarPorUrl(url);
            throw e;
        }
    }

    /**
     * Actualiza una marca existente y opcionalmente reemplaza su logotipo.
     *
     * @param id identificador de la marca
     * @param datos datos actualizados
     * @param imagen nuevo logotipo WebP opcional
     * @return marca actualizada
     */
    @Transactional
    public Marca actualizarMarca(Long id, NombreImagenAdminDTO datos, MultipartFile imagen) {
        Marca actual = marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La marca no existe"));
        validarNombre(datos.nombre(), "El nombre es obligatorio");
        String urlAnterior = actual.getLogoUrl();
        String urlNueva = subirImagenOpcional("marcas", datos.nombre(), imagen,
                "La imagen debe estar en formato .webp");
        try {
            actual.setNombre(datos.nombre().trim());
            if (urlNueva != null) actual.setLogoUrl(urlNueva);
            Marca guardada = marcaRepository.save(actual);
            if (urlNueva != null) storageService.eliminarPorUrl(urlAnterior);
            return guardada;
        } catch (RuntimeException e) {
            if (urlNueva != null) storageService.eliminarPorUrl(urlNueva);
            throw e;
        }
    }

    /**
     * Elimina una marca sin borrar sus productos asociados y limpia su logotipo.
     *
     * @param id identificador de la marca
     * @return URL del logotipo eliminado
     */
    @Transactional
    public String eliminarMarca(Long id) {
        var marca = marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La marca no existe"));
        for (Producto p : productoRepository.findByIdMarca(id)) {
            p.setIdMarca(null);
            productoRepository.save(p);
        }
        marcaRepository.delete(marca);
        storageService.eliminarPorUrl(marca.getLogoUrl());
        return marca.getLogoUrl();
    }

    /**
     * Lista todas las novedades ordenadas por identificador ascendente.
     *
     * @return lista completa de novedades
     */
    public List<Novedad> listarNovedades() {
        return novedadRepository.findAll(Sort.by("idNovedad").ascending());
    }

    /**
     * Crea una novedad validando la existencia del producto y el rango de fechas.
     *
     * @param d datos de la novedad
     * @return novedad persistida
     */
    @Transactional
    public Novedad crearNovedad(NovedadAdminDTO d) {
        if (d.idProducto() == null || !productoRepository.existsById(d.idProducto())) {
            throw new ValidacionException("Selecciona un producto válido");
        }
        validarRangoFechas(d.fechaInicio(), d.fechaFin());
        Novedad n = new Novedad();
        n.setIdProducto(d.idProducto());
        n.setFechaInicio(d.fechaInicio());
        n.setFechaFin(d.fechaFin());
        return novedadRepository.save(n);
    }

    /**
     * Actualiza las fechas de vigencia de una novedad existente.
     *
     * @param id identificador de la novedad
     * @param d datos actualizados
     * @return novedad actualizada
     */
    @Transactional
    public Novedad actualizarNovedad(Long id, NovedadAdminDTO d) {
        Novedad n = novedadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La novedad no existe"));
        validarRangoFechas(d.fechaInicio(), d.fechaFin());
        n.setFechaInicio(d.fechaInicio());
        n.setFechaFin(d.fechaFin());
        return novedadRepository.save(n);
    }

    /**
     * Elimina una novedad sin modificar el producto asociado.
     *
     * @param id identificador de la novedad
     */
    @Transactional
    public void eliminarNovedad(Long id) {
        if (!novedadRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("La novedad no existe");
        }
        novedadRepository.deleteById(id);
    }

    /**
     * Lista todas las ofertas ordenadas por identificador ascendente.
     *
     * @return lista completa de ofertas
     */
    public List<Oferta> listarOfertas() {
        return ofertaRepository.findAll(Sort.by("idOferta").ascending());
    }

    /**
     * Crea una oferta validando el producto, porcentaje de descuento y rango de fechas.
     *
     * @param d datos de la oferta
     * @return oferta persistida
     */
    @Transactional
    public Oferta crearOferta(OfertaAdminDTO d) {
        if (d.idProducto() == null || !productoRepository.existsById(d.idProducto())) {
            throw new ValidacionException("Selecciona un producto válido");
        }
        validarOferta(d);
        Oferta o = new Oferta();
        o.setIdProducto(d.idProducto());
        o.setActiva(true);
        aplicarOferta(o, d);
        return ofertaRepository.save(o);
    }

    /**
     * Actualiza una oferta existente.
     *
     * @param id identificador de la oferta
     * @param d datos actualizados
     * @return oferta actualizada
     */
    @Transactional
    public Oferta actualizarOferta(Long id, OfertaAdminDTO d) {
        Oferta o = ofertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La oferta no existe"));
        validarOferta(d);
        aplicarOferta(o, d);
        return ofertaRepository.save(o);
    }

    /**
     * Elimina una oferta sin modificar el producto asociado.
     *
     * @param id identificador de la oferta
     */
    @Transactional
    public void eliminarOferta(Long id) {
        if (!ofertaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("La oferta no existe");
        }
        ofertaRepository.deleteById(id);
    }

    private void validarOferta(OfertaAdminDTO d) {
        validarNombre(d.nombre(), "Escribe el nombre de la oferta");
        if (d.porcentajeDescuento() == null || d.porcentajeDescuento() <= 0 || d.porcentajeDescuento() >= 100) {
            throw new ValidacionException("El descuento debe estar entre 1 y 99 %");
        }
        validarRangoFechas(d.fechaInicio(), d.fechaFin());
    }

    private void aplicarOferta(Oferta o, OfertaAdminDTO d) {
        o.setNombre(d.nombre().trim());
        o.setPorcentajeDescuento(d.porcentajeDescuento());
        o.setFechaInicio(d.fechaInicio());
        o.setFechaFin(d.fechaFin());
    }

    private void validarNombre(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException(mensaje);
        }
    }

    private void validarRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            throw new ValidacionException("Indica la fecha de inicio y la fecha de fin");
        }
        if (!fin.isAfter(inicio)) {
            throw new ValidacionException("La fecha de fin debe ser posterior a la de inicio");
        }
    }

    private String subirImagenObligatoria(String carpeta, String nombreBase, MultipartFile imagen, String mensajeFormato) {
        if (!SupabaseStorageService.esWebp(imagen)) {
            throw new ValidacionException(mensajeFormato);
        }
        if (!storageService.estaConfigurado()) {
            throw new StorageNoConfiguradoException();
        }
        try {
            return storageService.subirWebp(carpeta, nombreBase, imagen);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new StorageException("No se pudo subir la imagen a Supabase: " + e.getMessage(), e);
        }
    }

    private String subirImagenOpcional(String carpeta, String nombreBase, MultipartFile imagen, String mensajeFormato) {
        if (imagen == null || imagen.isEmpty()) return null;
        return subirImagenObligatoria(carpeta, nombreBase, imagen, mensajeFormato);
    }
}
