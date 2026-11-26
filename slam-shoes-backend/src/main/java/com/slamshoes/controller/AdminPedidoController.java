package com.slamshoes.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.broker.Respuesta;
import com.slamshoes.service.BrokerGateway;

/**
 * Pedidos del panel del administrador. Igual que PedidoController, SOLO maneja HTTP: cada ruta se
 * convierte en una solicitud al servicio "pedidos" del Broker (ServicioPedidosJpa).
 *
 *   GET    /api/admin/pedidos?estado=PENDIENTE|CONCRETADO  -> pedidos.listarPedidosAdmin
 *   GET    /api/admin/pedidos/conteo                       -> pedidos.contarPedidos
 *   PUT    /api/admin/pedidos/{id}/concretar               -> pedidos.concretarPedido
 *   PUT    /api/admin/pedidos/{id}/reabrir                 -> pedidos.reabrirPedido
 *   DELETE /api/admin/pedidos/{id}                         -> pedidos.cancelarPedido (no concretado)
 */
@RestController
@RequestMapping("/api/admin/pedidos")
@CrossOrigin(origins = "*")
public class AdminPedidoController {

    @Autowired
    private BrokerGateway gateway;

    // Pedidos en proceso (por defecto) o concretados, con el nombre del cliente y sus productos
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(defaultValue = "PENDIENTE") String estado) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("estado", estado);
        Respuesta respuesta = gateway.enviar("pedidos", "listarPedidosAdmin", params);
        if (!respuesta.isExitosa()) {
            return RespuestaHttp.error(respuesta);
        }
        Object pedidos = RespuestaHttp.resultadoComoMapa(respuesta).get("pedidos");
        return ResponseEntity.ok(pedidos == null ? new ArrayList<>() : pedidos);
    }

    // Contadores para el panel: "total" es el numero de pedidos en proceso (el globo rojo del encabezado)
    @GetMapping("/conteo")
    public ResponseEntity<?> conteo() {
        Respuesta respuesta = gateway.enviar("pedidos", "contarPedidos", new LinkedHashMap<>());
        if (!respuesta.isExitosa()) {
            return RespuestaHttp.error(respuesta);
        }
        Map<String, Object> resultado = RespuestaHttp.resultadoComoMapa(respuesta);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("total", resultado.get("enProceso"));
        cuerpo.put("enProceso", resultado.get("enProceso"));
        cuerpo.put("concretados", resultado.get("concretados"));
        return ResponseEntity.ok(cuerpo);
    }

    @PutMapping("/{idPedido}/concretar")
    public ResponseEntity<?> concretar(@PathVariable Long idPedido) {
        return cambiarEstado("concretarPedido", idPedido);
    }

    // Deshace "concretado" (error de dedo): el pedido vuelve a la lista de en proceso
    @PutMapping("/{idPedido}/reabrir")
    public ResponseEntity<?> reabrir(@PathVariable Long idPedido) {
        return cambiarEstado("reabrirPedido", idPedido);
    }

    // "No concretado": se elimina el pedido y el stock regresa a la base de datos
    @DeleteMapping("/{idPedido}")
    public ResponseEntity<?> noConcretado(@PathVariable Long idPedido) {
        return cambiarEstado("cancelarPedido", idPedido);
    }

    private ResponseEntity<?> cambiarEstado(String operacion, Long idPedido) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPedido", idPedido);
        Respuesta respuesta = gateway.transicionarPedido(operacion, params);
        if (!respuesta.isExitosa()) {
            return RespuestaHttp.error(respuesta);
        }
        return ResponseEntity.ok(RespuestaHttp.resultadoComoMapa(respuesta));
    }

}
