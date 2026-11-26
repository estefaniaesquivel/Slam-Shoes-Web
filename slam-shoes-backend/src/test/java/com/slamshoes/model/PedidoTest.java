package com.slamshoes.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PedidoTest {

    @Test
    void permiteActualizarEstadoDelPedido() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);
        pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);

        pedido.setEstado(Pedido.EstadoPedido.CONCRETADO);
        assertThat(pedido.getEstado()).isEqualTo(Pedido.EstadoPedido.CONCRETADO);

        pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);
        assertThat(pedido.getEstado()).isEqualTo(Pedido.EstadoPedido.PENDIENTE);
    }

    @Test
    void permiteMantenerElEstadoActual() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);
        pedido.setEstado(Pedido.EstadoPedido.CONCRETADO);

        pedido.setEstado(Pedido.EstadoPedido.CONCRETADO);

        assertThat(pedido.getEstado()).isEqualTo(Pedido.EstadoPedido.CONCRETADO);
    }
}
