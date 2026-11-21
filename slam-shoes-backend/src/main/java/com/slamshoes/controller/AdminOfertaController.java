package com.slamshoes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.dto.OfertaAdminDTO;
import com.slamshoes.model.Oferta;
import com.slamshoes.service.AdminCatalogoService;

/**
 * Controlador REST para administrar las ofertas y descuentos aplicados a productos.
 */
@RestController
@RequestMapping("/api/admin/ofertas")
@CrossOrigin(origins = "*")
public class AdminOfertaController {

    private final AdminCatalogoService catalogoService;

    /**
     * Inicializa el controlador con el servicio administrativo de catálogo.
     *
     * @param catalogoService servicio de operaciones administrativas de catálogo
     */
    @Autowired
    public AdminOfertaController(AdminCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * Lista todas las ofertas (vigentes, programadas y vencidas).
     *
     * @return lista completa de ofertas
     */
    @GetMapping
    public List<Oferta> listar() {
        return catalogoService.listarOfertas();
    }

    /**
     * Crea una nueva oferta porcentual para un producto.
     *
     * @param d datos de la oferta
     * @return respuesta HTTP 201 con la oferta creada
     */
    @PostMapping
    public ResponseEntity<Oferta> crear(@RequestBody OfertaAdminDTO d) {
        return ResponseEntity.status(201).body(catalogoService.crearOferta(d));
    }

    /**
     * Actualiza los datos y vigencia de una oferta existente.
     *
     * @param id identificador de la oferta
     * @param d datos actualizados
     * @return respuesta HTTP 200 con la oferta actualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<Oferta> actualizar(@PathVariable Long id, @RequestBody OfertaAdminDTO d) {
        return ResponseEntity.ok(catalogoService.actualizarOferta(id, d));
    }

    /**
     * Elimina una oferta sin afectar al producto asociado.
     *
     * @param id identificador de la oferta
     * @return respuesta HTTP 204 sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        catalogoService.eliminarOferta(id);
        return ResponseEntity.noContent().build();
    }
}
