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

import com.slamshoes.dto.NovedadAdminDTO;
import com.slamshoes.model.Novedad;
import com.slamshoes.service.AdminCatalogoService;

@RestController
@RequestMapping("/api/admin/novedades")
@CrossOrigin(origins = "*")
public class AdminNovedadController {

    private final AdminCatalogoService catalogoService;

    @Autowired
    public AdminNovedadController(AdminCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public List<Novedad> listar() {
        return catalogoService.listarNovedades();
    }

    @PostMapping
    public ResponseEntity<Novedad> crear(@RequestBody NovedadAdminDTO d) {
        return ResponseEntity.status(201).body(catalogoService.crearNovedad(d));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Novedad> actualizar(@PathVariable Long id, @RequestBody NovedadAdminDTO d) {
        return ResponseEntity.ok(catalogoService.actualizarNovedad(id, d));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        catalogoService.eliminarNovedad(id);
        return ResponseEntity.noContent().build();
    }
}
