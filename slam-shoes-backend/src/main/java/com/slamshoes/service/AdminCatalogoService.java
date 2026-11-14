package com.slamshoes.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slamshoes.model.Coleccion;
import com.slamshoes.model.Producto;
import com.slamshoes.repository.ColeccionRepository;
import com.slamshoes.repository.JugadorRepository;
import com.slamshoes.repository.MarcaRepository;
import com.slamshoes.repository.ProductoRepository;

// Operaciones transaccionales de los módulos de administración que tocan varias tablas
@Service
public class AdminCatalogoService {

    @Autowired private ColeccionRepository coleccionRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private JugadorRepository jugadorRepository;
    @Autowired private MarcaRepository marcaRepository;

    // Crea (id == null) o actualiza una colección y deja en ella exactamente los productos indicados
    // (productos.id_coleccion). Un producto solo puede estar en una colección: si estaba en otra, se mueve.
    // imagenUrl == null significa "no cambiar la imagen".
    @Transactional
    public Coleccion guardarColeccion(Long id, String nombre, String descripcion, List<Long> idsProductos, String imagenUrl) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre de la colección es obligatorio");
        String nombreLimpio = nombre.trim();
        coleccionRepository.findByNombreIgnoreCase(nombreLimpio)
                .filter(otra -> !otra.getIdColeccion().equals(id))
                .ifPresent(otra -> { throw new IllegalArgumentException("Ya existe una colección con ese nombre"); });

        Set<Long> ids = new HashSet<>(idsProductos == null ? List.of() : idsProductos);
        List<Producto> elegidos = productoRepository.findAllById(ids);
        if (elegidos.size() != ids.size()) throw new IllegalArgumentException("Alguno de los productos elegidos ya no existe");

        Coleccion c;
        if (id == null) {
            c = new Coleccion();
            c.setActiva(true);
        } else {
            c = coleccionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("La colección no existe"));
        }
        c.setNombre(nombreLimpio);
        c.setDescripcion(descripcion == null ? "" : descripcion.trim());
        if (imagenUrl != null) c.setImagenUrl(imagenUrl);
        c = coleccionRepository.save(c);

        // Los que estaban y ya no están quedan sin colección
        if (id != null) {
            for (Producto p : productoRepository.findByIdColeccion(id)) {
                if (!ids.contains(p.getIdProducto())) {
                    p.setIdColeccion(null);
                    productoRepository.save(p);
                }
            }
        }
        for (Producto p : elegidos) {
            p.setIdColeccion(c.getIdColeccion());
            productoRepository.save(p);
        }
        return c;
    }

    // Quita al jugador del catálogo SIN borrar sus productos: solo se vacía productos.id_jugador
    @Transactional
    public String eliminarJugador(Long id) {
        var jugador = jugadorRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        for (Producto p : productoRepository.findByIdJugador(id)) {
            p.setIdJugador(null);
            productoRepository.save(p);
        }
        jugadorRepository.delete(jugador);
        return jugador.getImagenUrl();
    }

    // Igual para marcas: los productos se conservan y solo se vacía productos.id_marca
    @Transactional
    public String eliminarMarca(Long id) {
        var marca = marcaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("La marca no existe"));
        for (Producto p : productoRepository.findByIdMarca(id)) {
            p.setIdMarca(null);
            productoRepository.save(p);
        }
        marcaRepository.delete(marca);
        return marca.getLogoUrl();
    }
}
