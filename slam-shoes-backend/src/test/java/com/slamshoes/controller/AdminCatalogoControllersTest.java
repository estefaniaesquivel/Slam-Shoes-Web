package com.slamshoes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.slamshoes.exception.ValidacionException;
import com.slamshoes.model.Banner;
import com.slamshoes.model.Novedad;
import com.slamshoes.model.Oferta;
import com.slamshoes.service.AdminCatalogoService;

// REFACTOR: Move Method
// RAZON: Antes los 6 controladores Admin* (banners, colecciones, jugadores, marcas, novedades, ofertas) tenían repositorios
// y Storage inyectados y no se podían probar sin ellos. Ahora solo reciben AdminCatalogoService, así que esta clase los
// monta con MockMvc standalone y un único @Mock, y comprueba el contrato HTTP (RF-10, RF-11).
// REFACTOR: Replace Error Code with Exception
// RAZON: Antes cada controlador construía su 400/404 a mano. Ahora la prueba lanza ValidacionException desde el servicio
// simulado y verifica que GlobalExceptionHandler produce el 400 con {"error": mensaje} (RF-11).
@ExtendWith(MockitoExtension.class)
class AdminCatalogoControllersTest {

    @Mock
    private AdminCatalogoService catalogoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminBannerController(catalogoService),
                        new AdminColeccionController(catalogoService),
                        new AdminJugadorController(catalogoService),
                        new AdminMarcaController(catalogoService),
                        new AdminNovedadController(catalogoService),
                        new AdminOfertaController(catalogoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // REFACTOR: Move Method
    // RAZON: Verifica que AdminBannerController.listar y AdminNovedadController.crear solo delegan en el servicio
    // (listarBanners, crearNovedad) y responden 200 y 201 con el JSON esperado (RF-10, RF-11).
    @Test
    void listarBannersYCrearNovedad() throws Exception {
        Banner b = new Banner();
        b.setTitulo("Principal");
        when(catalogoService.listarBanners()).thenReturn(List.of(b));

        mockMvc.perform(get("/api/admin/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Principal"));

        Novedad n = new Novedad();
        n.setIdProducto(5L);
        when(catalogoService.crearNovedad(any())).thenReturn(n);

        mockMvc.perform(post("/api/admin/novedades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idProducto\":5,\"fechaInicio\":\"2026-11-20T00:00:00\",\"fechaFin\":\"2026-11-30T00:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto").value(5));
    }

    // REFACTOR: Replace Error Code with Exception
    // RAZON: Cubre el endpoint DELETE de colecciones agregado por Axel (204) y comprueba que una ValidacionException de
    // crearOferta se traduce en HTTP 400 mediante GlobalExceptionHandler, en lugar del antiguo validar() que devolvía
    // un String (RF-10, RF-11).
    @Test
    void eliminarColeccionRetorna204YOfertaInvalidaRetorna400() throws Exception {
        doNothing().when(catalogoService).eliminarColeccion(3L);
        when(catalogoService.crearOferta(any())).thenThrow(new ValidacionException("El descuento debe estar entre 1 y 99 %"));

        mockMvc.perform(delete("/api/admin/colecciones/3"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/admin/ofertas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idProducto\":1,\"nombre\":\"Test\",\"porcentajeDescuento\":150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El descuento debe estar entre 1 y 99 %"));

        Oferta o = new Oferta();
        o.setNombre("Hot Sale");
        when(catalogoService.listarOfertas()).thenReturn(List.of(o));
        mockMvc.perform(get("/api/admin/ofertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Hot Sale"));
    }
}
