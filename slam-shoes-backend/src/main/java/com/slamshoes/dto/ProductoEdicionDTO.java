package com.slamshoes.dto;

import java.util.List;

// REFACTOR: Extract Interface
// RAZON: Antes este record repetía por su cuenta los 11 campos comunes de producto que también tenían ProductoNuevoDTO y
// ProductoAdminDetalleDTO. Ahora implementa ProductoBaseDTO y solo agrega lo propio (imagenes), de modo que
// ProductoAdminService.validarComunes(ProductoBaseDTO, ...) lo acepta igual que a los otros dos.
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
        List<ImagenAdminDTO> imagenes) implements ProductoBaseDTO {}
