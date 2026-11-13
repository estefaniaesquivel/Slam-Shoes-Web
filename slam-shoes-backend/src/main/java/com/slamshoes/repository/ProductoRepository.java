package com.slamshoes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Productos (tenis) indexados a un jugador mediante productos.id_jugador
    List<Producto> findByIdJugador(Long idJugador);

    // Productos indexados a una colección mediante productos.id_coleccion
    List<Producto> findByIdColeccion(Long idColeccion);

    // Productos de una marca (productos.id_marca)
    List<Producto> findByIdMarca(Long idMarca);

    // Productos del mismo modelo (columna productos.modelo), sin distinguir mayúsculas
    List<Producto> findByModeloIgnoreCase(String modelo);

    // Productos de una categoría (productos.id_categoria)
    List<Producto> findByIdCategoria(Long idCategoria);
}
