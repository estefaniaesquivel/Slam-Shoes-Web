package com.slamshoes.dto;

import java.util.List;

// Contrato común de atributos entre creación, edición y detalle de productos administrativos
public interface ProductoBaseDTO {
    String nombre();
    String descripcion();
    Double precio();
    Long idCategoria();
    Long idMarca();
    String coleccion();
    String jugador();
    Boolean activo();
    String modelo();
    Boolean esTendencia();
    List<ColorTallasDTO> colores();
}
