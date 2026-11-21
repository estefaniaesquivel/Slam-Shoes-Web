package com.slamshoes.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.dto.ProductoAdminDetalleDTO;
import com.slamshoes.dto.ProductoEdicionDTO;
import com.slamshoes.dto.ProductoNuevoDTO;
import com.slamshoes.service.IProductoAdminService;

/**
 * Controlador REST para la creación, edición, consulta de detalle y baja de productos.
 */
@RestController
@RequestMapping("/api/admin/productos")
@CrossOrigin(origins = "*")
public class AdminProductoController {

    private final IProductoAdminService productoAdminService;

    /**
     * Inicializa el controlador con el servicio de administración de productos.
     *
     * @param productoAdminService servicio de gestión administrativa de productos
     */
    @Autowired
    public AdminProductoController(IProductoAdminService productoAdminService) {
        this.productoAdminService = productoAdminService;
    }

    /**
     * Obtiene el detalle completo de un producto para el formulario de edición.
     *
     * @param id identificador del producto
     * @return respuesta HTTP 200 con el detalle completo
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoAdminDetalleDTO> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(productoAdminService.obtenerDetalle(id));
    }

    /**
     * Crea un producto nuevo junto con sus variantes e imágenes en formato WebP.
     *
     * @param datos datos del producto y variantes
     * @param imagenes lista ordenada de archivos WebP
     * @return respuesta HTTP 201 con el identificador, nombre y carpeta asignada
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> crear(@RequestPart("producto") ProductoNuevoDTO datos,
                                                     @RequestPart("imagenes") List<MultipartFile> imagenes) {
        return ResponseEntity.status(201).body(productoAdminService.crearCompleto(datos, imagenes));
    }

    /**
     * Edita un producto existente, sincroniza sus variantes y actualiza su galería.
     *
     * @param id identificador del producto
     * @param datos datos actualizados y orden final de galería
     * @param nuevas imágenes nuevas opcionales para las posiciones sin URL
     * @return respuesta HTTP 200 con el identificador y nombre actualizado
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> editar(@PathVariable Long id,
                                                      @RequestPart("producto") ProductoEdicionDTO datos,
                                                      @RequestPart(value = "imagenes", required = false) List<MultipartFile> nuevas) {
        return ResponseEntity.ok(productoAdminService.editarCompleto(id, datos, nuevas));
    }

    /**
     * Elimina un producto si no tiene historial en pedidos.
     *
     * @param id identificador del producto
     * @return respuesta HTTP 204 sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoAdminService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
