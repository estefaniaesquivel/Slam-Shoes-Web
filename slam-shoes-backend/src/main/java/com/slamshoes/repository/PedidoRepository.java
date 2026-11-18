package com.slamshoes.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByIdUsuario(Long idUsuario, Sort sort);

    // Panel del administrador: pedidos en proceso (PENDIENTE) o concretados (CONCRETADO)
    List<Pedido> findByEstado(String estado, Sort sort);

    long countByEstado(String estado);
}