package com.slamshoes.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.slamshoes.model.enums.EstadoPedido;

// Pedido tal como lo ve el cliente en "Mis pedidos". Lo arma PedidoService.pedidosDeUsuario.
public record PedidoClienteDTO(
        Long idPedido,
        LocalDateTime fecha,
        Double total,
        EstadoPedido estado,
        String direccionEnvio,
        String metodoPago,
        List<LineaPedidoDTO> lineas) {

    // Una línea del pedido: el producto comprado con su talla, color, cantidad y precio unitario
    public record LineaPedidoDTO(
            Long idProducto,
            String nombre,
            String imagenUrl,
            String talla,
            String color,
            Integer cantidad,
            Double precioUnitario) {}
}