package com.slamshoes.dto;

import java.time.LocalDateTime;

// Una fila = campos de pedidos + campos de pedido_detalle
public record PedidoFilaDTO(
        Long idPedido,
        Long idUsuario,
        LocalDateTime fecha,
        Double total,
        String estado,
        String direccionEnvio,
        String metodoPago,
        Long idDetalle,
        Long idVariante,
        Integer cantidad,
        Double precioUnitario) {}
