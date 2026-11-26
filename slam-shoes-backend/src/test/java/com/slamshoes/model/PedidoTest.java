package com.slamshoes.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.slamshoes.exception.TransicionEstadoInvalidaException;
import com.slamshoes.model.enums.EstadoPedido;

class PedidoTest {

    @Test
    void cambiaEstadoSoloMedianteAccionesDeDominio() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);

        pedido.concretar();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CONCRETADO);

        pedido.reabrir();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);

        pedido.marcarNoConcretado();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.NO_CONCRETADO);
    }

    @Test
    void rechazaAccionNoPermitidaSinAlterarElEstado() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);
        pedido.concretar();

        assertThatThrownBy(pedido::concretar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessage("El pedido #7 no está en proceso (estado actual: CONCRETADO)");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CONCRETADO);
    }
}
