package com.slamshoes.dto;

import java.time.LocalDateTime;

public record OfertaAdminDTO(Long idProducto, String nombre, Double porcentajeDescuento,
                             LocalDateTime fechaInicio, LocalDateTime fechaFin) {}
