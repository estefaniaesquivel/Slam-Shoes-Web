package com.slamshoes.dto;

import java.util.List;

// REFACTOR: Extract Interface
// RAZON: Antes ProductoNuevoDTO, ProductoEdicionDTO y ProductoAdminDetalleDTO repetían los mismos 11 campos de
// producto/colores (nombre, descripcion, precio, idCategoria, idMarca, coleccion, jugador, activo, modelo,
// esTendencia, colores). Ahora ese contrato vive aquí (un record no puede heredar de una clase, por eso se usa una
// interfaz en lugar de Extract Superclass) y los tres records lo implementan.
// REFACTOR: Introduce Parameter Object
// RAZON: Antes ProductoAdminService/AdminProductoController validaban con validarComunes(nombre, precio, idCategoria,
// idMarca, coleccion, jugador, colores, coloresActuales), 8 parámetros sueltos. Ahora validarComunes y
// aplicarDatosComunes reciben un solo ProductoBaseDTO.
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
