package com.slamshoes.dto;

import java.util.List;

public record PedidoRequestDTO(Long idUsuario, String direccionEnvio, String metodoPago, List<ItemPedidoDTO> items) {}
