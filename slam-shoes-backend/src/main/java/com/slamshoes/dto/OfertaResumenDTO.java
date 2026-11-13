package com.slamshoes.dto;

import java.time.LocalDateTime;

// Resumen ligero de la mejor oferta de cada producto (para mostrar la etiqueta de descuento en cualquier tarjeta)
public record OfertaResumenDTO(
        Long idProducto,
        String nombre,
        Double porcentajeDescuento,
        Double precioConDescuento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean vigente) {}
