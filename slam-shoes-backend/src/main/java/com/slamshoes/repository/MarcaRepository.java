package com.slamshoes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Marca;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {

    List<Marca> findByActivaTrueOrderByNombreAsc();
}
