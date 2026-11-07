package com.slamshoes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Jugador;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    // Para el menú "Por Jugador": todos los jugadores de la BD en orden alfabético
    List<Jugador> findAllByOrderByNombreAsc();
}