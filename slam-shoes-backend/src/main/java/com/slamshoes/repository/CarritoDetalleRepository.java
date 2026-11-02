package com.slamshoes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.slamshoes.model.CarritoDetalle;

@Repository
public interface CarritoDetalleRepository extends JpaRepository<CarritoDetalle, Long> {
    List<CarritoDetalle> findByIdCarrito(Long idCarrito);

    // Borrado masivo explícito por id_carrito usando JPQL
    @Modifying
    @Transactional
    @Query("DELETE FROM CarritoDetalle c WHERE c.idCarrito = :idCarrito")
    void deleteByIdCarrito(@Param("idCarrito") Long idCarrito);

    @Modifying
    @Transactional
    @Query("DELETE FROM CarritoDetalle c WHERE c.idCarrito = :idCarrito AND c.idVariante = :idVariante")
    void deleteByIdCarritoAndIdVariante(@Param("idCarrito") Long idCarrito, @Param("idVariante") Long idVariante);
}