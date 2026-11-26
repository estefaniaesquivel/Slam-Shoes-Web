package com.slamshoes.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.slamshoes.dto.PedidoClienteDTO.LineaPedidoDTO;
import com.slamshoes.model.enums.EstadoPedido;

// Una fila del panel del administrador = un pedido con el nombre del cliente y todas sus líneas.
// Lo arma PedidoService.listarAdmin.
public record PedidoFilaDTO(
        Long idPedido,
        LocalDateTime fecha,
        Double total,
        EstadoPedido estado,
        String direccionEnvio,
        String metodoPago,
        Long idUsuario,
        String nombreCliente,
        List<LineaPedidoDTO> lineas) {}