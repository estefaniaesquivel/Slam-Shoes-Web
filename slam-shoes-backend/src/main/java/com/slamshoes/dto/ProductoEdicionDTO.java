package com.slamshoes.dto;

import java.util.List;

public record ProductoEdicionDTO(
        String nombre,
        String descripcion,
        Double precio,
        Long idCategoria,
        Long idMarca,
        String coleccion,
        String jugador,
        Boolean activo,
        String modelo,
        Boolean esTendencia,
        List<ColorTallasDTO> colores,
        List<ImagenAdminDTO> imagenes) {}
