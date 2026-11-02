package com.slamshoes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.ProductoTalla;

@Repository
public interface ProductoTallaRepository extends JpaRepository<ProductoTalla, Long> {
    List<ProductoTalla> findByIdProducto(Long idProducto);
}