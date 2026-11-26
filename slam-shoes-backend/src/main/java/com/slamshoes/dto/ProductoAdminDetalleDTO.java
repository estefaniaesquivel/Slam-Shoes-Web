package com.slamshoes.dto;

import java.util.List;

// Todo lo guardado de un producto, para rellenar el formulario de edición
// REFACTOR: Extract Interface
// RAZON: Antes este record repetía por su cuenta los 11 campos comunes de producto que también tenían ProductoNuevoDTO y
// ProductoEdicionDTO. Ahora implementa ProductoBaseDTO y solo agrega lo propio (idProducto e imagenes), de modo que la
// misma validación sirve para los tres.
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
        List<ImagenAdminDTO> imagenes) implements ProductoBaseDTO {}
