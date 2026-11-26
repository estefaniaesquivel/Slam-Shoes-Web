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

// REFACTOR: Move Method
// RAZON: Antes el controlador inyectaba JugadorRepository, AdminCatalogoService y SupabaseStorageService (3 @Autowired) y
// repetía, igual que AdminMarcaController, la validación, subida WebP y rollback de imagen en 121 líneas. Ahora cada
// endpoint delega en AdminCatalogoService (listarJugadores, crearJugador, actualizarJugador, eliminarJugador) y la
// clase queda en 92 líneas.
// REFACTOR: Replace Error Code with Exception
// RAZON: Antes los métodos devolvían ResponseEntity<?> y armaban a mano los 400/404/500/502 con error() y sinLlave().
// Ahora el servicio lanza ValidacionException, RecursoNoEncontradoException, StorageNoConfiguradoException o
// StorageException y GlobalExceptionHandler produce la respuesta HTTP.
/**
 * Controlador REST para la administración de jugadores del catálogo.
 */
@RestController
@RequestMapping("/api/admin/jugadores")
@CrossOrigin(origins = "*")
public class AdminJugadorController {

    private final AdminCatalogoService catalogoService;

    // REFACTOR: Remove Setting Method
    // RAZON: Antes las dependencias eran campos @Autowired privados, inyectados por reflexión y sin forma de crear el
    // controlador fuera de Spring. Ahora es un campo final asignado solo en el constructor, y las pruebas hacen
    // new AdminJugadorController(catalogoService) sin levantar contexto.
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
