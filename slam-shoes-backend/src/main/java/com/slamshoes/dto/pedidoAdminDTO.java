package com.slamshoes.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.slamshoes.dto.PedidoClienteDTO.LineaPedidoDTO;

// REFACTOR: Replace Record with Data Class (tipado fuerte en lugar de Map)
// RAZON: Antes el panel del administrador recibía Map<String, Object> armados a mano con claves de texto
// ("nombreCliente", "lineas"...) dentro de ServicioPedidosJpa; un error de ortografía en una clave pasaba
// inadvertido. Ahora es un record con campos tipados que Jackson convierte al mismo JSON de siempre.
/**
 * Pedido tal como lo ve el panel del administrador: datos del pedido, nombre del cliente y sus productos (RF-27, RF-30).
 *
 * @param idPedido       identificador del pedido
 * @param idUsuario      cliente que lo hizo
 * @param nombreCliente  nombre del cliente ("Cliente #id" si ya no existe)
 * @param fecha          fecha y hora de creación
 * @param total          total cobrado
 * @param estado         nombre del {@code EstadoPedido}
 * @param direccionEnvio dirección de entrega
 * @param metodoPago     método de pago (puede ser nulo)
 * @param lineas         productos del pedido
 */
record PedidoAdminDTO(
        Long idPedido,
        Long idUsuario,
        String nombreCliente,
        LocalDateTime fecha,
        Double total,
        String estado,
        String direccionEnvio,
        String metodoPago,
        List<LineaPedidoDTO> lineas) {}
