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
import com.slamshoes.model.Jugador;
import com.slamshoes.service.AdminCatalogoService;

/**
 * Controlador REST para la administración de jugadores del catálogo.
 */
@RestController
@RequestMapping("/api/admin/jugadores")
@CrossOrigin(origins = "*")
public class AdminJugadorController {

    private final AdminCatalogoService catalogoService;

    /**
     * Inicializa el controlador con el servicio administrativo de catálogo.
     *
     * @param catalogoService servicio de operaciones administrativas de catálogo
     */
    @Autowired
    public AdminJugadorController(AdminCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * Lista todos los jugadores ordenados alfabéticamente.
     *
     * @return lista completa de jugadores
     */
    @GetMapping
    public List<Jugador> listar() {
        return catalogoService.listarJugadores();
    }

    /**
     * Registra un nuevo jugador con su imagen WebP.
     *
     * @param datos nombre del jugador
     * @param imagen imagen WebP del jugador
     * @return respuesta HTTP 201 con el jugador creado
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Jugador> crear(@RequestPart("datos") NombreImagenAdminDTO datos,
                                         @RequestPart("imagen") MultipartFile imagen) {
        return ResponseEntity.status(201).body(catalogoService.crearJugador(datos, imagen));
    }

    /**
     * Actualiza un jugador existente y opcionalmente reemplaza su imagen.
     *
     * @param id identificador del jugador
     * @param datos datos actualizados
     * @param imagen nueva imagen WebP opcional
     * @return respuesta HTTP 200 con el jugador actualizado
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Jugador> actualizar(@PathVariable Long id,
                                              @RequestPart("datos") NombreImagenAdminDTO datos,
                                              @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.ok(catalogoService.actualizarJugador(id, datos, imagen));
    }

    /**
     * Elimina un jugador desvinculando sus productos asociados.
     *
     * @param id identificador del jugador
     * @return respuesta HTTP 204 sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        catalogoService.eliminarJugador(id);
        return ResponseEntity.noContent().build();
    }
}
