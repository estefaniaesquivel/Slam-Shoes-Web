package com.slamshoes.dto;

import java.util.List;

public record ColeccionAdminDetalleDTO(Long idColeccion, String nombre, String descripcion, String imagenUrl,
                                       List<ProductoMiniDTO> productos) {}
