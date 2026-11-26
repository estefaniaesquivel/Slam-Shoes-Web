package com.slamshoes.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.slamshoes.service.BrokerGateway;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private BrokerGateway gateway;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PedidoController(gateway))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void pedidoSinProductosRetorna400ConMensajeDeError() throws Exception {
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
}
