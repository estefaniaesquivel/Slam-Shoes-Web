package com.slamshoes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.slamshoes.dto.BannerAdminDTO;
import com.slamshoes.dto.ColeccionAdminDTO;
import com.slamshoes.dto.NovedadAdminDTO;
import com.slamshoes.dto.OfertaAdminDTO;
import com.slamshoes.exception.RecursoNoEncontradoException;
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

// REFACTOR: Move Method
// RAZON: Antes la validacion, la subida de imagen y el borrado de banners, novedades, ofertas, jugadores, marcas y colecciones
// vivian dentro de los controladores Admin*, imposibles de probar sin levantar la capa web. Al moverlas a AdminCatalogoService estas pruebas
// usan Mockito sobre repositorios y almacenamiento falso, sin Spring (RF-10).
// REFACTOR: Replace Error Code with Exception
// RAZON: Antes los errores se devolvian como String o ResponseEntity con el mensaje; ahora los casos de error se verifican
// con assertThatThrownBy(...) sobre ValidacionException, RecursoNoEncontradoException y ConflictoEliminacionException (RF-10).
@ExtendWith(MockitoExtension.class)
class AdminCatalogoServiceTest {

    @Mock private ColeccionRepository coleccionRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private JugadorRepository jugadorRepository;
    @Mock private MarcaRepository marcaRepository;
    @Mock private BannerRepository bannerRepository;
    @Mock private NovedadRepository novedadRepository;
    @Mock private OfertaRepository ofertaRepository;
    @Mock private IStorageService storageService;

    private AdminCatalogoService service;

    @BeforeEach
    void setUp() {
        service = new AdminCatalogoService(
                coleccionRepository, productoRepository, jugadorRepository, marcaRepository,
                bannerRepository, novedadRepository, ofertaRepository, storageService);
    }

    @Test
    void crearColeccionSubeImagenYAsignaProductos() throws Exception {
        when(storageService.estaConfigurado()).thenReturn(true);
        when(storageService.subirWebp(anyString(), anyString(), any())).thenReturn("https://cdn/col.webp");
        when(coleccionRepository.findByNombreIgnoreCase("Retro")).thenReturn(Optional.empty());
        Producto p = new Producto();
        p.setIdProducto(10L);
        when(productoRepository.findAllById(any())).thenReturn(List.of(p));
        when(coleccionRepository.save(any(Coleccion.class))).thenAnswer(inv -> {
            Coleccion c = inv.getArgument(0);
            c.setIdColeccion(5L);
            return c;
        });

        MockMultipartFile img = new MockMultipartFile("imagen", "retro.webp", "image/webp", new byte[]{1});
        Coleccion creada = service.crearColeccion(new ColeccionAdminDTO("Retro", "Desc", List.of(10L)), img);

        assertThat(creada.getIdColeccion()).isEqualTo(5L);
        assertThat(p.getIdColeccion()).isEqualTo(5L);
        verify(productoRepository).save(p);
    }

    // REFACTOR: Replace Error Code with Exception
    // RAZON: Antes el controlador devolvia el error con ResponseEntity/error(String); ahora guardarColeccion lanza ValidacionException ante un nombre duplicado y la prueba lo exige.
    @Test
    void guardarColeccionRechazaNombreDuplicado() {
        Coleccion existente = new Coleccion();
        existente.setIdColeccion(2L);
        when(coleccionRepository.findByNombreIgnoreCase("Retro")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.guardarColeccion(1L, "Retro", "", List.of(), null))
                .isInstanceOf(ValidacionException.class);
    }

    // REFACTOR: Move Method
    // RAZON: La regla de desvincular productos y borrar la imagen ahora vive en AdminCatalogoService.eliminarColeccion, por eso se prueba aqui y no en el controlador.
    @Test
    void eliminarColeccionDesvinculaProductosYBorraImagen() {
        Coleccion c = new Coleccion();
        c.setIdColeccion(3L);
        c.setImagenUrl("https://cdn/col.webp");
        Producto p = new Producto();
        p.setIdProducto(7L);
        p.setIdColeccion(3L);

        when(coleccionRepository.findById(3L)).thenReturn(Optional.of(c));
        when(productoRepository.findByIdColeccion(3L)).thenReturn(List.of(p));

        service.eliminarColeccion(3L);

        assertThat(p.getIdColeccion()).isNull();
        verify(coleccionRepository).delete(c);
        verify(storageService).eliminarPorUrl("https://cdn/col.webp");
    }

    // REFACTOR: Replace Error Code with Exception
    // RAZON: Ahora un id inexistente lanza RecursoNoEncontradoException (404 en GlobalExceptionHandler) en lugar de responder con un mensaje de error armado en el controlador.
    @Test
    void eliminarJugadorYMarcaInexistentesLanzanRecursoNoEncontrado() {
        when(jugadorRepository.findById(99L)).thenReturn(Optional.empty());
        when(marcaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarJugador(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.eliminarMarca(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarJugadorLimpiaProductos() {
        Jugador j = new Jugador();
        j.setIdJugador(4L);
        j.setImagenUrl("https://cdn/j.webp");
        Producto p = new Producto();
        p.setIdJugador(4L);
        when(jugadorRepository.findById(4L)).thenReturn(Optional.of(j));
        when(productoRepository.findByIdJugador(4L)).thenReturn(List.of(p));

        service.eliminarJugador(4L);

        assertThat(p.getIdJugador()).isNull();
        verify(jugadorRepository).delete(j);
        verify(storageService).eliminarPorUrl("https://cdn/j.webp");
    }

    @Test
    void eliminarMarcaLimpiaProductos() {
        Marca m = new Marca();
        m.setIdMarca(8L);
        m.setLogoUrl("https://cdn/m.webp");
        Producto p = new Producto();
        p.setIdMarca(8L);
        when(marcaRepository.findById(8L)).thenReturn(Optional.of(m));
        when(productoRepository.findByIdMarca(8L)).thenReturn(List.of(p));

        service.eliminarMarca(8L);

        assertThat(p.getIdMarca()).isNull();
        verify(marcaRepository).delete(m);
        verify(storageService).eliminarPorUrl("https://cdn/m.webp");
    }

    @Test
    void crearBannerAsignaOrdenSiguiente() throws Exception {
        when(storageService.estaConfigurado()).thenReturn(true);
        when(storageService.subirWebp(anyString(), anyString(), any())).thenReturn("https://cdn/b.webp");
        Banner existente = new Banner();
        existente.setOrden(3);
        when(bannerRepository.findAll()).thenReturn(List.of(existente));
        when(bannerRepository.save(any(Banner.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile img = new MockMultipartFile("imagen", "b.webp", "image/webp", new byte[]{1});
        Banner guardado = service.crearBanner(new BannerAdminDTO("Hero", "/catalogo", null, true), img);

        assertThat(guardado.getOrden()).isEqualTo(4);
        assertThat(guardado.getImagenUrl()).isEqualTo("https://cdn/b.webp");
    }

    @Test
    void crearNovedadYOfertaValidanFechasYPorcentaje() {
        LocalDateTime ahora = LocalDateTime.now();
        when(productoRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.crearNovedad(new NovedadAdminDTO(1L, ahora, ahora.minusDays(1))))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> service.crearOferta(new OfertaAdminDTO(1L, "Promo", 120.0, ahora, ahora.plusDays(2))))
                .isInstanceOf(ValidacionException.class);

        when(novedadRepository.save(any(Novedad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(inv -> inv.getArgument(0));

        Novedad nov = service.crearNovedad(new NovedadAdminDTO(1L, ahora, ahora.plusDays(5)));
        Oferta of = service.crearOferta(new OfertaAdminDTO(1L, "Buen Fin", 25.0, ahora, ahora.plusDays(5)));

        assertThat(nov.getIdProducto()).isEqualTo(1L);
        assertThat(of.getPorcentajeDescuento()).isEqualTo(25.0);
    }
}
