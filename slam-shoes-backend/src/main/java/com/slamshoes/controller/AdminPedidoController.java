package com.slamshoes.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.dto.PedidoFilaDTO;
import com.slamshoes.model.Pedido;
import com.slamshoes.model.PedidoDetalle;
import com.slamshoes.repository.PedidoDetalleRepository;
import com.slamshoes.repository.PedidoRepository;

@RestController
@RequestMapping("/api/admin/pedidos")
@CrossOrigin(origins = "*")
public class AdminPedidoController {

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PedidoDetalleRepository pedidoDetalleRepository;

    // Todas las columnas de pedidos + pedido_detalle en una sola lista (una fila por producto del pedido)
    @GetMapping
    public List<PedidoFilaDTO> listarFilas() {
        List<PedidoFilaDTO> filas = new ArrayList<>();
        for (Pedido p : pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "fecha"))) {
            List<PedidoDetalle> detalles = pedidoDetalleRepository.findByIdPedido(p.getIdPedido());
            if (detalles.isEmpty()) {
                filas.add(new PedidoFilaDTO(p.getIdPedido(), p.getIdUsuario(), p.getFecha(), p.getTotal(),
                        p.getEstado(), p.getDireccionEnvio(), p.getMetodoPago(), null, null, null, null));
            }
            for (PedidoDetalle d : detalles) {
                filas.add(new PedidoFilaDTO(p.getIdPedido(), p.getIdUsuario(), p.getFecha(), p.getTotal(),
                        p.getEstado(), p.getDireccionEnvio(), p.getMetodoPago(),
                        d.getIdDetalle(), d.getIdVariante(), d.getCantidad(), d.getPrecioUnitario()));
            }
        }
        return filas;
    }

    // Contador de pedidos para el ícono del encabezado del administrador
    @GetMapping("/conteo")
    public Map<String, Long> conteo() {
        return Map.of("total", pedidoRepository.count());
    }
}
