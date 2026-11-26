package com.slamshoes.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Pedido;
import com.slamshoes.model.enums.EstadoPedido;
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByIdUsuario(Long idUsuario, Sort sort);

    // Panel del administrador: pedidos en proceso (PENDIENTE) o concretados (CONCRETADO)
    // REFACTOR: Replace Type Code with Class (enum)
    // RAZON: Antes recibían el estado como String libre; ahora reciben EstadoPedido y no se puede consultar un valor inventado.
    List<Pedido> findByEstado(EstadoPedido estado, Sort sort);

    long countByEstado(EstadoPedido estado);
}