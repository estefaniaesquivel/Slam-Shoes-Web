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

@RestController
@RequestMapping("/api/admin/productos")
@CrossOrigin(origins = "*")
public class AdminProductoController {

    private final IProductoAdminService productoAdminService;

    @Autowired
    public AdminProductoController(IProductoAdminService productoAdminService) {
        this.productoAdminService = productoAdminService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoAdminDetalleDTO> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(productoAdminService.obtenerDetalle(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> crear(@RequestPart("producto") ProductoNuevoDTO datos,
                                                     @RequestPart("imagenes") List<MultipartFile> imagenes) {
        return ResponseEntity.status(201).body(productoAdminService.crearCompleto(datos, imagenes));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> editar(@PathVariable Long id,
                                                      @RequestPart("producto") ProductoEdicionDTO datos,
                                                      @RequestPart(value = "imagenes", required = false) List<MultipartFile> nuevas) {
        return ResponseEntity.ok(productoAdminService.editarCompleto(id, datos, nuevas));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoAdminService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
