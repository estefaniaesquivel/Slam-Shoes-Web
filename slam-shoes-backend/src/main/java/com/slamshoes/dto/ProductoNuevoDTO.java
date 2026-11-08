package com.slamshoes.dto;

import java.util.List;

// Datos del formulario "Añadir producto al catálogo" (las imágenes viajan aparte, como archivos)
public record ProductoNuevoDTO(
        String nombre,
        String descripcion,
        Double precio,
        Long idCategoria,
        Long idMarca,
        String coleccion,   // nombre; vacío = sin colección
        String jugador,     // nombre; vacío = sin jugador
        Boolean activo,
        String modelo,
        Boolean esTendencia,
        List<ColorTallasDTO> colores) {}
