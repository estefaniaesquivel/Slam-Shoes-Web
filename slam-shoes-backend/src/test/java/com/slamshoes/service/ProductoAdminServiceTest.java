package com.slamshoes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.slamshoes.dto.ColorTallasDTO;
import com.slamshoes.dto.ImagenAdminDTO;
import com.slamshoes.dto.ProductoAdminDetalleDTO;
import com.slamshoes.dto.ProductoEdicionDTO;
import com.slamshoes.dto.ProductoNuevoDTO;
import com.slamshoes.dto.TallaStockDTO;
import com.slamshoes.exception.ConflictoEliminacionException;
import com.slamshoes.model.Marca;
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

@ExtendWith(MockitoExtension.class)
class ProductoAdminServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private ProductoTallaRepository productoTallaRepository;
    @Mock private ProductoImagenRepository productoImagenRepository;
    @Mock private PedidoDetalleRepository pedidoDetalleRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private MarcaRepository marcaRepository;
    @Mock private ColeccionRepository coleccionRepository;
    @Mock private JugadorRepository jugadorRepository;
    @Mock private IStorageService storageService;

    private ProductoAdminService service;

    @BeforeEach
    void setUp() {
        service = new ProductoAdminService(
                productoRepository, productoTallaRepository, productoImagenRepository,
                pedidoDetalleRepository, categoriaRepository, marcaRepository,
                coleccionRepository, jugadorRepository, storageService);
    }

    @Test
    void obtenerDetalleAgrupaColoresEImagenesOrdenadas() {
        Producto p = new Producto();
        p.setIdProducto(1L);
        p.setNombre("Jordan 1");
        p.setPrecio(3200.0);
        p.setIdCategoria(2L);
        p.setIdMarca(3L);

        ProductoTalla t1 = new ProductoTalla();
        t1.setColor("Rojo");
        t1.setTalla("27");
        t1.setStock(5);

        ProductoImagen i2 = new ProductoImagen();
        i2.setUrlImagen("https://cdn/2.webp");
        i2.setOrden(2);
        ProductoImagen i1 = new ProductoImagen();
        i1.setUrlImagen("https://cdn/1.webp");
        i1.setOrden(1);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productoTallaRepository.findByIdProducto(1L)).thenReturn(List.of(t1));
        when(productoImagenRepository.findByIdProducto(1L)).thenReturn(List.of(i2, i1));

        ProductoAdminDetalleDTO dto = service.obtenerDetalle(1L);

        assertThat(dto.idProducto()).isEqualTo(1L);
        assertThat(dto.colores()).hasSize(1);
        assertThat(dto.imagenes()).extracting(ImagenAdminDTO::orden).containsExactly(1, 2);
    }

    @Test
    void crearCompletoGuardaProductoVariantesEImagenes() throws Exception {
        Marca m = new Marca();
        m.setIdMarca(3L);
        m.setNombre("Nike");

        when(categoriaRepository.existsById(2L)).thenReturn(true);
        when(marcaRepository.findById(3L)).thenReturn(Optional.of(m));
        when(storageService.estaConfigurado()).thenReturn(true);
        when(storageService.subir(anyString(), anyString(), any(), anyString())).thenReturn("https://cdn/1.webp");
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setIdProducto(99L);
            return p;
        });

        ProductoNuevoDTO dto = new ProductoNuevoDTO(
                "Nike Dunk", "Desc", 2500.0, 2L, 3L, "", "", true, "Dunk", false,
                List.of(new ColorTallasDTO("Negro", List.of(new TallaStockDTO("27", 4)))));
        MockMultipartFile img = new MockMultipartFile("imagenes", "1.webp", "image/webp", new byte[]{1});

        Map<String, Object> res = service.crearCompleto(dto, List.of(img));

        assertThat(res.get("idProducto")).isEqualTo(99L);
        verify(productoTallaRepository).save(any(ProductoTalla.class));
        verify(productoImagenRepository).save(any(ProductoImagen.class));
    }

    @Test
    void actualizarDejaStockCeroEnVarianteConPedidos() {
        Producto p = new Producto();
        p.setIdProducto(1L);

        ProductoTalla existenteConPedido = new ProductoTalla();
        existenteConPedido.setIdVariante(50L);
        existenteConPedido.setColor("Azul");
        existenteConPedido.setTalla("26");
        existenteConPedido.setStock(3);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productoRepository.save(any(Producto.class))).thenReturn(p);
        when(productoTallaRepository.findByIdProducto(1L)).thenReturn(List.of(existenteConPedido));
        when(pedidoDetalleRepository.existsByIdVariante(50L)).thenReturn(true);

        ProductoEdicionDTO dto = new ProductoEdicionDTO(
                "Nike Dunk", "Desc", 2500.0, 2L, 3L, "", "", true, "Dunk", false,
                List.of(new ColorTallasDTO("Negro", List.of(new TallaStockDTO("27", 4)))),
                List.of(new ImagenAdminDTO("https://cdn/1.webp", 1)));

        service.actualizar(1L, dto, "Nike", null, null, List.of("https://cdn/1.webp"));

        assertThat(existenteConPedido.getStock()).isZero();
        verify(productoTallaRepository, never()).delete(existenteConPedido);
    }

    @Test
    void eliminarLanzaConflictoSiTienePedidos() {
        ProductoTalla pt = new ProductoTalla();
        pt.setIdVariante(70L);
        when(productoRepository.existsById(1L)).thenReturn(true);
        when(productoTallaRepository.findByIdProducto(1L)).thenReturn(List.of(pt));
        when(pedidoDetalleRepository.existsByIdVariante(70L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoEliminacionException.class);
        verify(productoRepository, never()).deleteById(any());
    }

    @Test
    void eliminarBorraProductoEImagenesSiNoTienePedidos() {
        ProductoTalla pt = new ProductoTalla();
        pt.setIdVariante(71L);
        ProductoImagen img = new ProductoImagen();
        img.setUrlImagen("https://cdn/1.webp");

        when(productoRepository.existsById(1L)).thenReturn(true);
        when(productoTallaRepository.findByIdProducto(1L)).thenReturn(List.of(pt));
        when(pedidoDetalleRepository.existsByIdVariante(71L)).thenReturn(false);
        when(productoImagenRepository.findByIdProducto(1L)).thenReturn(List.of(img));
        when(storageService.rutaDesdeUrl("https://cdn/1.webp")).thenReturn("productos/1.webp");
        when(storageService.estaConfigurado()).thenReturn(true);

        service.eliminar(1L);

        verify(productoRepository).deleteById(1L);
        verify(storageService).eliminar(anyList());
    }
}
