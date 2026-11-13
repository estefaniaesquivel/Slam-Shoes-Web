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

import com.slamshoes.dto.VarianteModeloDTO;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.ProductoRepository;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*") // Permite la comunicación con el frontend de Vue.js
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    // 1. READ: Obtener todos los productos (para la tienda y el panel admin)
    @GetMapping
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // 2. READ BY ID: Obtener un producto específico para cuando el admin pulse "Editar"
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProductoPorId(@PathVariable Long id) {
        return productoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Variantes de diseño: los productos activos con el mismo "modelo" (incluye al propio producto).
    // Cada uno se muestra con su portada (imagen de orden 1 = productos.imagen_url).
    @GetMapping("/{id}/variantes")
    public ResponseEntity<List<VarianteModeloDTO>> variantes(@PathVariable Long id) {
        return productoRepository.findById(id).map(actual -> {
            String modelo = actual.getModelo() == null ? "" : actual.getModelo().trim();
            List<Producto> mismos = modelo.isEmpty()
                    ? List.of(actual)
                    : productoRepository.findByModeloIgnoreCase(modelo);
            List<VarianteModeloDTO> lista = mismos.stream()
                    .filter(p -> p.getIdProducto().equals(id) || !Boolean.FALSE.equals(p.getActivo()))
                    .sorted((a, b) -> Long.compare(a.getIdProducto(), b.getIdProducto()))
                    .map(p -> new VarianteModeloDTO(p.getIdProducto(), p.getNombre(), p.getImagenUrl()))
                    .toList();
            return ResponseEntity.ok(lista);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 3. CREATE: Registrar un nuevo producto desde el formulario del administrador
    @PostMapping
    public Producto guardarProducto(@RequestBody Producto producto) {
        return productoRepository.save(producto);
    }

    // 4. UPDATE: Modificar un producto existente
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable Long id, @RequestBody Producto productoDetalles) {
        return productoRepository.findById(id).map(producto -> {
            producto.setNombre(productoDetalles.getNombre());
            producto.setMarca(productoDetalles.getMarca());
            producto.setDescripcion(productoDetalles.getDescripcion());
            producto.setPrecio(productoDetalles.getPrecio());
            producto.setImagenUrl(productoDetalles.getImagenUrl());
            producto.setIdCategoria(productoDetalles.getIdCategoria());
            producto.setIdJugador(productoDetalles.getIdJugador());
            producto.setIdColeccion(productoDetalles.getIdColeccion()); // Mapeo del ID de la colección
            producto.setIdMarca(productoDetalles.getIdMarca());
            producto.setActivo(productoDetalles.getActivo());
            
            producto.setModelo(productoDetalles.getModelo());
            producto.setEsTendencia(productoDetalles.getEsTendencia());
            
            Producto productoActualizado = productoRepository.save(producto);
            return ResponseEntity.ok(productoActualizado);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. DELETE: Eliminar un producto del sistema
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarProducto(@PathVariable Long id) {
        return productoRepository.findById(id).map(producto -> {
            productoRepository.delete(producto);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}