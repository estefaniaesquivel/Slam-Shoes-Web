package com.slamshoes.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.dto.ProductoAdminDetalleDTO;
import com.slamshoes.dto.ProductoEdicionDTO;
import com.slamshoes.dto.ProductoNuevoDTO;

// REFACTOR: Extract Interface
// RAZON: Antes AdminProductoController dependía de la clase concreta ProductoAdminService (más 8 repositorios y el servicio
// de Storage), por lo que no se podía probar sin ellos. Ahora el controlador depende de esta interfaz de 4 operaciones
// (obtenerDetalle, crearCompleto, editarCompleto, eliminar) y ProductoAdminService es solo una implementación.
/**
 * Contrato para la gestión administrativa completa de productos, variantes y galería.
 */
public interface IProductoAdminService {

    /**
     * Obtiene el detalle completo de un producto para rellenar el formulario de edición.
     *
     * @param id identificador del producto
     * @return DTO con los datos del producto, variantes agrupadas por color e imágenes
     */
    ProductoAdminDetalleDTO obtenerDetalle(Long id);

    /**
     * Valida datos, sube imágenes WebP y crea el producto junto con sus tallas y galería.
     *
     * @param datos datos del nuevo producto
     * @param imagenes archivos de imagen en formato WebP
     * @return mapa con {@code idProducto}, {@code nombre} y {@code carpeta}
     */
    Map<String, Object> crearCompleto(ProductoNuevoDTO datos, List<MultipartFile> imagenes);

    /**
     * Valida datos, actualiza la galería de imágenes, sincroniza tallas y guarda cambios del producto.
     *
     * @param id identificador del producto a editar
     * @param datos datos actualizados del producto
     * @param nuevas archivos nuevos para las posiciones sin URL previa
     * @return mapa con {@code idProducto} y {@code nombre}
     */
    Map<String, Object> editarCompleto(Long id, ProductoEdicionDTO datos, List<MultipartFile> nuevas);

    /**
     * Elimina un producto si no tiene variantes asociadas a pedidos históricos y limpia sus imágenes.
     *
     * @param idProducto identificador del producto a eliminar
     */
    void eliminar(Long idProducto);
}
