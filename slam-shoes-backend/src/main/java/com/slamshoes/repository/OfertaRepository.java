package com.slamshoes.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Oferta;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    // Ofertas vigentes: activas y con "ahora" dentro del periodo inicio-fin
    List<Oferta> findByActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            LocalDateTime inicio, LocalDateTime fin);
}
