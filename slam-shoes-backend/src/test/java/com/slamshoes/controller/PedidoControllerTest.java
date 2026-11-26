package com.slamshoes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyMap;
import org.mockito.Mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.slamshoes.exception.StockInsuficienteException;
import com.slamshoes.exception.TransicionEstadoInvalidaException;
import com.slamshoes.service.BrokerGateway;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private BrokerGateway gateway;

    private MockMvc mockMvc;

    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PedidoController(gateway))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void pedidoSinProductosRetorna400ConMensajeDeError() throws Exception {
                setUp();

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"direccionEnvio\":\"Calle 1\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El pedido no tiene productos"));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"direccionEnvio\":\"Calle 1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El pedido no tiene productos"));

        verifyNoInteractions(gateway);
    }

    @Test
    void stockInsuficienteRetorna409ConMensajeDeError() throws Exception {
                setUp();

        when(gateway.crearPedido(anyMap()))
                .thenThrow(new StockInsuficienteException("Sin stock suficiente de \"Runner\" talla 27 (disponibles: 0)"));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"direccionEnvio\":\"Calle 1\",\"items\":[{\"idVariante\":2,\"cantidad\":1}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Sin stock suficiente de \"Runner\" talla 27 (disponibles: 0)"));
    }

    @Test
    void transicionDeEstadoInvalidaRetorna409ConMensajeDeError() {
                setUp();

        var response = new GlobalExceptionHandler().manejarTransicionEstadoInvalida(
                new TransicionEstadoInvalidaException("El pedido no está en proceso"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).containsEntry("error", "El pedido no está en proceso");
    }
}
