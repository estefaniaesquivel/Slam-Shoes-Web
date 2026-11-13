package com.slamshoes.dto;

import java.time.LocalDateTime;

import com.slamshoes.model.Producto;

public record OfertaProductoDTO(
        Long idOferta,
        String nombre,
        Double porcentajeDescuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean vigente,
        Double precioConDescuento,
        Producto producto) {}
