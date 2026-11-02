package com.slamshoes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Favorito;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByIdUsuario(Long idUsuario);
    void deleteByIdUsuarioAndIdProducto(Long idUsuario, Long idProducto);
}