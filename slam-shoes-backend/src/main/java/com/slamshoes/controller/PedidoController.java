package com.slamshoes.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.broker.Respuesta;
import com.slamshoes.dto.ItemPedidoDTO;
import com.slamshoes.dto.PedidoRequestDTO;
import com.slamshoes.service.BrokerGateway;

/**
 * Controlador de pedidos: AHORA SOLO maneja HTTP. Recibe la peticion, la convierte en una solicitud
 * al Broker y devuelve la respuesta. La validacion de usuario y stock, el calculo de precios con
 * ofertas, el guardado y el descuento de existencias viven en el proveedor "pedidos"
 * (ServicioPedidosJpa). Las rutas y las respuestas son las mismas de antes.
 */
@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired
    private BrokerGateway gateway;

    // Confirmar compra -> pedidos.crearPedido
    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody PedidoRequestDTO req) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (req != null) {
            params.put("idUsuario", req.idUsuario());
            params.put("direccionEnvio", req.direccionEnvio());
            params.put("metodoPago", req.metodoPago());
            List<Map<String, Object>> items = new ArrayList<>();
            if (req.items() != null) {
                for (ItemPedidoDTO item : req.items()) {
                    Map<String, Object> linea = new LinkedHashMap<>();
                    linea.put("idVariante", item == null ? null : item.idVariante());
                    linea.put("cantidad", item == null ? null : item.cantidad());
                    items.add(linea);
                }
            }
            params.put("items", items);
        }

        Respuesta respuesta = gateway.enviar("pedidos", "crearPedido", params);
        if (!respuesta.isExitosa()) {
            return RespuestaHttp.error(respuesta);
        }
        Map<String, Object> resultado = RespuestaHttp.resultadoComoMapa(respuesta);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idPedido", resultado.get("idPedido"));
        cuerpo.put("total", resultado.get("total"));
        cuerpo.put("estado", resultado.get("estado"));
        return ResponseEntity.status(201).body(cuerpo);
    }

    // Mis pedidos -> pedidos.consultarHistorial (mas recientes primero)
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<?> pedidosDeUsuario(@PathVariable Long idUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idUsuario", idUsuario);
        Respuesta respuesta = gateway.enviar("pedidos", "consultarHistorial", params);
        if (!respuesta.isExitosa()) {
            return RespuestaHttp.error(respuesta);
        }
        Object pedidos = RespuestaHttp.resultadoComoMapa(respuesta).get("pedidos");
        return ResponseEntity.ok(pedidos == null ? new ArrayList<>() : pedidos);
    }
}
