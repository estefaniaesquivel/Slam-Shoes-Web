package com.slamshoes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.slamshoes.dto.ProductoAdminDetalleDTO;
import com.slamshoes.exception.ConflictoEliminacionException;
import com.slamshoes.exception.RecursoNoEncontradoException;
import com.slamshoes.service.IProductoAdminService;

@ExtendWith(MockitoExtension.class)
class AdminProductoControllerTest {

    @Mock
    private IProductoAdminService productoAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProductoController(productoAdminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void detalleRetorna200Y404CuandoNoExiste() throws Exception {
        ProductoAdminDetalleDTO dto = new ProductoAdminDetalleDTO(
                1L, "Jordan 1", "Desc", 2900.0, 1L, 2L, "", "", true, "J1", false, List.of(), List.of());
        when(productoAdminService.obtenerDetalle(1L)).thenReturn(dto);
        when(productoAdminService.obtenerDetalle(99L)).thenThrow(new RecursoNoEncontradoException("El producto no existe"));

        mockMvc.perform(get("/api/admin/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Jordan 1"));

        mockMvc.perform(get("/api/admin/productos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El producto no existe"));
    }

    @Test
    void crearRetorna201() throws Exception {
        when(productoAdminService.crearCompleto(any(), anyList()))
                .thenReturn(Map.of("idProducto", 10L, "nombre", "Kobe 6", "carpeta", "kobe-6"));

        String json = "{\"nombre\":\"Kobe 6\",\"precio\":3500,\"idCategoria\":1,\"idMarca\":1,\"colores\":[{\"color\":\"Negro\",\"tallas\":[{\"talla\":\"27\",\"stock\":2}]}]}";
        MockMultipartFile productoPart = new MockMultipartFile("producto", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
        MockMultipartFile imgPart = new MockMultipartFile("imagenes", "1.webp", "image/webp", new byte[]{1});

        mockMvc.perform(multipart("/api/admin/productos").file(productoPart).file(imgPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto").value(10));
    }

    @Test
    void eliminarRetorna204Y409EnConflicto() throws Exception {
        doNothing().when(productoAdminService).eliminar(1L);
        doThrow(new ConflictoEliminacionException("Ya aparece en pedidos"))
                .when(productoAdminService).eliminar(eq(2L));

        mockMvc.perform(delete("/api/admin/productos/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/admin/productos/2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya aparece en pedidos"));
    }
}
