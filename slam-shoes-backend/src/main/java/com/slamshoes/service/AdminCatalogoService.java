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

    // ---------------------------------------------------------------- Colecciones
    public List<Coleccion> listarColecciones() {
        return coleccionRepository.findAll(Sort.by("nombre").ascending());
    }

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

    // ---------------------------------------------------------------- Banners
    public List<Banner> listarBanners() {
        return bannerRepository.findAll(Sort.by("orden").ascending().and(Sort.by("idBanner").ascending()));
    }

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

    // ---------------------------------------------------------------- Jugadores
    public List<Jugador> listarJugadores() {
        return jugadorRepository.findAll(Sort.by("nombre").ascending());
    }

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

    // ---------------------------------------------------------------- Marcas
    public List<Marca> listarMarcas() {
        return marcaRepository.findAll(Sort.by("nombre").ascending());
    }

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

    // ---------------------------------------------------------------- Novedades
    public List<Novedad> listarNovedades() {
        return novedadRepository.findAll(Sort.by("idNovedad").ascending());
    }

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

    @Transactional
    public Novedad actualizarNovedad(Long id, NovedadAdminDTO d) {
        Novedad n = novedadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La novedad no existe"));
        validarRangoFechas(d.fechaInicio(), d.fechaFin());
        n.setFechaInicio(d.fechaInicio());
        n.setFechaFin(d.fechaFin());
        return novedadRepository.save(n);
    }

    @Transactional
    public void eliminarNovedad(Long id) {
        if (!novedadRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("La novedad no existe");
        }
        novedadRepository.deleteById(id);
    }

    // ---------------------------------------------------------------- Ofertas
    public List<Oferta> listarOfertas() {
        return ofertaRepository.findAll(Sort.by("idOferta").ascending());
    }

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

    @Transactional
    public Oferta actualizarOferta(Long id, OfertaAdminDTO d) {
        Oferta o = ofertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La oferta no existe"));
        validarOferta(d);
        aplicarOferta(o, d);
        return ofertaRepository.save(o);
    }

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

    // ---------------------------------------------------------------- Helpers compartidos
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
