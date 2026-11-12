package com.slamshoes.dto;

import java.util.List;

// Todo lo guardado de un producto, para rellenar el formulario de edición
public record ProductoAdminDetalleDTO(
        Long idProducto,
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
