package com.slamshoes.dto;

// Misma forma que los artículos que el frontend guarda en localStorage
public record CarritoItemDTO(
        Long id,
        Long idVariante,
        String nombre,
        Double precio,
        String imagenUrl,
        String talla,
        String color,
        Integer cantidad) {}
