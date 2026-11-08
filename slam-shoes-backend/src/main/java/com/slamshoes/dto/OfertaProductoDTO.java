package com.slamshoes.dto;

import java.time.LocalDateTime;

import com.slamshoes.model.Producto;

public record OfertaProductoDTO(
        Long idOferta,
        String nombre,
        Double porcentajeDescuento,
        LocalDateTime fechaFin,
        Double precioConDescuento,
        Producto producto) {}
