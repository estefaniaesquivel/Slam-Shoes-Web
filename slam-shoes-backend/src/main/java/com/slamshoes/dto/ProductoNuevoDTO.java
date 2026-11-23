package com.slamshoes.dto;

import java.util.List;

// Datos del formulario "Añadir producto al catálogo" (las imágenes viajan aparte, como archivos)
// REFACTOR: Extract Interface
// RAZON: Antes este record repetía por su cuenta los mismos 11 campos comunes de producto que ProductoEdicionDTO y
// ProductoAdminDetalleDTO. Ahora implementa ProductoBaseDTO (sin campos propios, porque las imágenes viajan como
// archivos aparte), y la validación del alta usa el mismo contrato que la edición.
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
        List<ColorTallasDTO> colores) implements ProductoBaseDTO {}
