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

// REFACTOR: Move Method
// RAZON: Antes el controlador inyectaba NovedadRepository y ProductoRepository (2 @Autowired) y hacía el CRUD directo
// contra los repositorios, con su propio validar(). Ahora delega en AdminCatalogoService (listarNovedades,
// crearNovedad, actualizarNovedad, eliminarNovedad) y no conoce repositorios.
// REFACTOR: Replace Error Code with Exception
// RAZON: Antes validar(NovedadAdminDTO) devolvía un String con el problema (o null) y error(String) armaba el 400; la
// ausencia de la novedad se resolvía con ResponseEntity.notFound(). Ahora el servicio lanza ValidacionException o
// RecursoNoEncontradoException y GlobalExceptionHandler responde 400/404.
/**
 * Controlador REST para administrar las vigencias de productos marcados como novedad.
 */
@RestController
@RequestMapping("/api/admin/novedades")
@CrossOrigin(origins = "*")
public class AdminNovedadController {

    private final AdminCatalogoService catalogoService;

    // REFACTOR: Remove Setting Method
    // RAZON: Antes las dependencias eran campos @Autowired privados, inyectados por reflexión y sin forma de crear el
    // controlador fuera de Spring. Ahora es un campo final asignado solo en el constructor, y las pruebas hacen
    // new AdminNovedadController(catalogoService) sin levantar contexto.
    /**
     * Inicializa el controlador con el servicio administrativo de catálogo.
     *
     * @param catalogoService servicio de operaciones administrativas de catálogo
     */
    @Autowired
    public AdminNovedadController(AdminCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * Lista todas las novedades registradas.
     *
     * @return lista completa de novedades
     */
    @GetMapping
    public List<Novedad> listar() {
        return catalogoService.listarNovedades();
    }

    /**
     * Registra una nueva novedad para un producto dentro de un rango de fechas.
     *
     * @param d datos de la novedad
     * @return respuesta HTTP 201 con la novedad creada
     */
    @PostMapping
    public ResponseEntity<Novedad> crear(@RequestBody NovedadAdminDTO d) {
        return ResponseEntity.status(201).body(catalogoService.crearNovedad(d));
    }

    /**
     * Actualiza las fechas de vigencia de una novedad existente.
     *
     * @param id identificador de la novedad
     * @param d datos actualizados
     * @return respuesta HTTP 200 con la novedad actualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<Novedad> actualizar(@PathVariable Long id, @RequestBody NovedadAdminDTO d) {
        return ResponseEntity.ok(catalogoService.actualizarNovedad(id, d));
    }

    /**
     * Elimina una novedad sin afectar al producto asociado.
     *
     * @param id identificador de la novedad
     * @return respuesta HTTP 204 sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        catalogoService.eliminarNovedad(id);
        return ResponseEntity.noContent().build();
    }
}
