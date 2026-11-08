package com.slamshoes.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Novedad;

@Repository
public interface NovedadRepository extends JpaRepository<Novedad, Long> {

    // Novedades vigentes: "ahora" dentro del periodo inicio-fin
    List<Novedad> findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaInicioDesc(
            LocalDateTime inicio, LocalDateTime fin);
}
