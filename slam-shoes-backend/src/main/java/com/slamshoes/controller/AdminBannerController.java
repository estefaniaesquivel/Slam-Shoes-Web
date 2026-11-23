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

import com.slamshoes.dto.BannerAdminDTO;
import com.slamshoes.model.Banner;
import com.slamshoes.service.AdminCatalogoService;

// REFACTOR: Move Method
// RAZON: Antes el controlador inyectaba BannerRepository y SupabaseStorageService (2 @Autowired) y contenía validación,
// subida WebP, rollback de imagen y armado de respuestas en 127 líneas. Ahora cada endpoint solo delega en
// AdminCatalogoService (listarBanners, crearBanner, actualizarBanner, eliminarBanner) y la clase queda en 92 líneas.
// REFACTOR: Replace Error Code with Exception
// RAZON: Antes crear/actualizar devolvían ResponseEntity<?> y armaban a mano los 400/404/500/502 con los métodos privados
// error() y sinLlave(). Ahora el servicio lanza ValidacionException, RecursoNoEncontradoException,
// StorageNoConfiguradoException o StorageException, GlobalExceptionHandler responde, y los métodos devuelven tipos concretos.
/**
 * Controlador REST para la administración de banners del carrusel principal.
 */
@RestController
@RequestMapping("/api/admin/banners")
@CrossOrigin(origins = "*")
public class AdminBannerController {

    private final AdminCatalogoService catalogoService;

    // REFACTOR: Remove Setting Method
    // RAZON: Antes la dependencia era un campo @Autowired privado, inyectado por reflexión y sin forma de crear el
    // controlador fuera de Spring. Ahora es un campo final asignado solo en el constructor, y las pruebas hacen
    // new AdminBannerController(catalogoService) sin levantar contexto.
    /**
     * Inicializa el controlador con el servicio administrativo de catálogo.
     *
     * @param catalogoService servicio de operaciones administrativas de catálogo
     */
    @Autowired
    public AdminBannerController(AdminCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * Lista todos los banners ordenados por posición e identificador.
     *
     * @return lista completa de banners
     */
    @GetMapping
    public List<Banner> listar() {
        return catalogoService.listarBanners();
    }

    /**
     * Crea un nuevo banner subiendo su imagen WebP al almacenamiento.
     *
     * @param datos metadatos del banner
     * @param imagen archivo de imagen en formato WebP
     * @return respuesta HTTP 201 con el banner creado
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Banner> crear(@RequestPart("banner") BannerAdminDTO datos,
                                        @RequestPart("imagen") MultipartFile imagen) {
        return ResponseEntity.status(201).body(catalogoService.crearBanner(datos, imagen));
    }

    /**
     * Actualiza un banner existente y opcionalmente reemplaza su imagen.
     *
     * @param id identificador del banner
     * @param datos metadatos actualizados
     * @param imagen nueva imagen WebP opcional
     * @return respuesta HTTP 200 con el banner actualizado
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Banner> actualizar(@PathVariable Long id,
                                             @RequestPart("banner") BannerAdminDTO datos,
                                             @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.ok(catalogoService.actualizarBanner(id, datos, imagen));
    }

    /**
     * Elimina un banner y su imagen asociada del almacenamiento.
     *
     * @param id identificador del banner
     * @return respuesta HTTP 204 sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        catalogoService.eliminarBanner(id);
        return ResponseEntity.noContent().build();
    }
}
