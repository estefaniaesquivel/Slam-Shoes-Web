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

import com.slamshoes.dto.ColeccionAdminDTO;
import com.slamshoes.dto.ColeccionAdminDetalleDTO;
import com.slamshoes.model.Coleccion;
import com.slamshoes.service.AdminCatalogoService;

// REFACTOR: Move Method
// RAZON: Antes el controlador inyectaba 4 dependencias (@Autowired) y contenía validación, subida de portada, rollback
// y armado de respuestas en 126 líneas. Ahora cada endpoint delega en AdminCatalogoService (listarColecciones,
// detalleColeccion, crearColeccion, actualizarColeccion, eliminarColeccion) y la clase queda en 107 líneas.
// REFACTOR: Replace Error Code with Exception
// RAZON: Antes los métodos devolvían ResponseEntity<?> y armaban a mano los 400/404/500/502 con error() y sinLlave().
// Ahora el servicio lanza ValidacionException, RecursoNoEncontradoException, StorageNoConfiguradoException o
// StorageException y GlobalExceptionHandler produce la respuesta HTTP.
/**
 * Controlador REST para la administración de colecciones y sus productos vinculados.
 */
@RestController
@RequestMapping("/api/admin/colecciones")
@CrossOrigin(origins = "*")
public class AdminColeccionController {

    private final AdminCatalogoService catalogoService;

    // REFACTOR: Remove Setting Method
    // RAZON: Antes las dependencias eran campos @Autowired privados, inyectados por reflexión y sin forma de crear el
    // controlador fuera de Spring. Ahora es un campo final asignado solo en el constructor, y las pruebas hacen
    // new AdminColeccionController(catalogoService) sin levantar contexto.
    /**
     * Inicializa el controlador con el servicio administrativo de catálogo.
     *
     * @param catalogoService servicio de operaciones administrativas de catálogo
     */
    @Autowired
    public AdminColeccionController(AdminCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * Lista todas las colecciones ordenadas alfabéticamente.
     *
     * @return lista completa de colecciones
     */
    @GetMapping
    public List<Coleccion> listar() {
        return catalogoService.listarColecciones();
    }

    /**
     * Obtiene el detalle de una colección junto con sus productos asociados.
     *
     * @param id identificador de la colección
     * @return detalle administrativo de la colección
     */
    @GetMapping("/{id}")
    public ResponseEntity<ColeccionAdminDetalleDTO> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoService.detalleColeccion(id));
    }

    /**
     * Crea una colección con portada WebP y asigna sus productos.
     *
     * @param datos datos y lista de identificadores de productos
     * @param imagen portada WebP de la colección
     * @return respuesta HTTP 201 con {@code idColeccion} y {@code nombre}
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> crear(@RequestPart("coleccion") ColeccionAdminDTO datos,
                                                     @RequestPart("imagen") MultipartFile imagen) {
        Coleccion c = catalogoService.crearColeccion(datos, imagen);
        return ResponseEntity.status(201).body(Map.of("idColeccion", c.getIdColeccion(), "nombre", c.getNombre()));
    }

    /**
     * Actualiza una colección existente, su portada opcional y sus productos vinculados.
     *
     * @param id identificador de la colección
     * @param datos datos actualizados
     * @param imagen nueva portada WebP opcional
     * @return respuesta HTTP 200 con {@code idColeccion} y {@code nombre}
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable Long id,
                                                          @RequestPart("coleccion") ColeccionAdminDTO datos,
                                                          @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        Coleccion c = catalogoService.actualizarColeccion(id, datos, imagen);
        return ResponseEntity.ok(Map.of("idColeccion", c.getIdColeccion(), "nombre", c.getNombre()));
    }

    // REFACTOR: Move Method
    // RAZON: Antes el controlador no tenía endpoint DELETE para colecciones (faltante señalado en el plan, Integrante 5).
    // Ahora eliminar() solo delega en AdminCatalogoService.eliminarColeccion, que desvincula los productos y borra la
    // portada, igual que ya hacían jugadores y marcas, sin lógica de negocio en el controlador.
    /**
     * Elimina una colección desvinculando sus productos sin borrarlos.
     *
     * @param id identificador de la colección
     * @return respuesta HTTP 204 sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        catalogoService.eliminarColeccion(id);
        return ResponseEntity.noContent().build();
    }
}
