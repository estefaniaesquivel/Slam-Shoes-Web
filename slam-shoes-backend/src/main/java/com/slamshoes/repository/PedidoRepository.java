package com.slamshoes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}