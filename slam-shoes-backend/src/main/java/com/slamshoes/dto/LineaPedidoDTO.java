package com.slamshoes.dto;

/**
 * Una línea de un pedido: el producto comprado con su talla, color, cantidad y precio unitario.
 */
public record LineaPedidoDTO(
        Long idProducto,
        String nombre,
        String imagenUrl,
        String talla,
        String color,
        Integer cantidad,
        Double precioUnitario) {
}
