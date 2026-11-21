package com.slamshoes.controller;

import java.util.List;

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

import com.slamshoes.dto.NombreImagenAdminDTO;
import com.slamshoes.model.Marca;
import com.slamshoes.service.AdminCatalogoService;

/**
 * Controlador REST para la administración de marcas del catálogo.
 */
@RestController
@RequestMapping("/api/admin/marcas")
@CrossOrigin(origins = "*")
public class AdminMarcaController {

    private final AdminCatalogoService catalogoService;

    /**
     * Inicializa el controlador con el servicio administrativo de catálogo.
     *
     * @param catalogoService servicio de operaciones administrativas de catálogo
     */
    @Autowired
    public AdminMarcaController(AdminCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * Lista todas las marcas ordenadas alfabéticamente.
     *
     * @return lista completa de marcas
     */
    @GetMapping
    public List<Marca> listar() {
        return catalogoService.listarMarcas();
    }

    /**
     * Crea una nueva marca con su logotipo en formato WebP.
     *
     * @param datos nombre de la marca
     * @param imagen logotipo en formato WebP
     * @return respuesta HTTP 201 con la marca creada
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Marca> crear(@RequestPart("datos") NombreImagenAdminDTO datos,
                                       @RequestPart("imagen") MultipartFile imagen) {
        return ResponseEntity.status(201).body(catalogoService.crearMarca(datos, imagen));
    }

    /**
     * Actualiza una marca existente y opcionalmente reemplaza su logotipo.
     *
     * @param id identificador de la marca
     * @param datos datos actualizados
     * @param imagen nuevo logotipo WebP opcional
     * @return respuesta HTTP 200 con la marca actualizada
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Marca> actualizar(@PathVariable Long id,
                                            @RequestPart("datos") NombreImagenAdminDTO datos,
                                            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.ok(catalogoService.actualizarMarca(id, datos, imagen));
    }

    /**
     * Elimina una marca desvinculando sus productos asociados.
     *
     * @param id identificador de la marca
     * @return respuesta HTTP 204 sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        catalogoService.eliminarMarca(id);
        return ResponseEntity.noContent().build();
    }
}
