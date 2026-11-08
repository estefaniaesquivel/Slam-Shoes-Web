package com.slamshoes.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoClienteDTO(
        Long idPedido,
        LocalDateTime fecha,
        Double total,
        String estado,
        String direccionEnvio,
        String metodoPago,
        List<LineaPedidoDTO> lineas) {

    public record LineaPedidoDTO(
            Long idProducto,
            String nombre,
            String imagenUrl,
            String talla,
            String color,
            Integer cantidad,
            Double precioUnitario) {}
}
