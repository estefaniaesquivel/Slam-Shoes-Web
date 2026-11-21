package com.slamshoes.dto;

import java.util.List;

/**
 * Contrato común de atributos compartidos entre creación, edición y detalle administrativo de productos.
 */
public interface ProductoBaseDTO {

    /** @return nombre comercial del producto */
    String nombre();

    /** @return descripción detallada del producto */
    String descripcion();

    /** @return precio unitario en moneda local */
    Double precio();

    /** @return identificador de la categoría asignada */
    Long idCategoria();

    /** @return identificador de la marca asignada */
    Long idMarca();

    /** @return nombre de la colección opcional */
    String coleccion();

    /** @return nombre del jugador opcional */
    String jugador();

    /** @return indicador de visibilidad del producto en el catálogo */
    Boolean activo();

    /** @return identificador de modelo para agrupar variantes */
    String modelo();

    /** @return indicador de producto destacado en tendencia */
    Boolean esTendencia();

    /** @return lista de colores con sus tallas y existencias */
    List<ColorTallasDTO> colores();
}
