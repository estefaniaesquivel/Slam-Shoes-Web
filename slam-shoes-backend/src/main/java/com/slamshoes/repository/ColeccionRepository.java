package com.slamshoes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Coleccion;

@Repository
public interface ColeccionRepository extends JpaRepository<Coleccion, Long> {

    // Solo colecciones activas, ordenadas por nombre (menú y Home)
    List<Coleccion> findByActivaTrueOrderByNombreAsc();

    Optional<Coleccion> findByNombreIgnoreCase(String nombre);
}
