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

@RestController
@RequestMapping("/api/admin/ofertas")
@CrossOrigin(origins = "*")
public class AdminOfertaController {

    private final AdminCatalogoService catalogoService;

    @Autowired
    public AdminOfertaController(AdminCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public List<Oferta> listar() {
        return catalogoService.listarOfertas();
    }

    @PostMapping
    public ResponseEntity<Oferta> crear(@RequestBody OfertaAdminDTO d) {
        return ResponseEntity.status(201).body(catalogoService.crearOferta(d));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Oferta> actualizar(@PathVariable Long id, @RequestBody OfertaAdminDTO d) {
        return ResponseEntity.ok(catalogoService.actualizarOferta(id, d));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        catalogoService.eliminarOferta(id);
        return ResponseEntity.noContent().build();
    }
}
