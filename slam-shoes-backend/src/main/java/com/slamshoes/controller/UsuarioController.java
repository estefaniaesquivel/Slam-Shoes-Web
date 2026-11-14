package com.slamshoes.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.broker.Respuesta;
import com.slamshoes.dto.RegistroDTO;
import com.slamshoes.service.BrokerGateway;

/**
 * Controlador de usuarios: AHORA SOLO maneja HTTP. La autenticacion (BCrypt) y el registro (rol
 * siempre CLIENTE, hash nunca expuesto) viven en el proveedor "usuarios" (ServicioUsuariosJpa).
 * Las rutas y las respuestas son las mismas de antes.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173") // Permite la conexión con tu frontend de Vue
public class UsuarioController {

    @Autowired
    private BrokerGateway gateway;

    // Login -> usuarios.autenticar (200 con el usuario, 401 si las credenciales no coinciden)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("correo", credentials == null ? null : credentials.get("correo"));
        params.put("contrasena", credentials == null ? null : credentials.get("contrasena"));

        Respuesta respuesta = gateway.enviar("usuarios", "autenticar", params);
        if (!respuesta.isExitosa()) {
            return RespuestaHttp.error(respuesta);
        }
        return ResponseEntity.ok(RespuestaHttp.resultadoComoMapa(respuesta));
    }

    // Registro público de clientes nuevos -> usuarios.registrar (201, 400 o 409 si el correo ya existe)
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroDTO datos) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("nombre", datos == null ? null : datos.getNombre());
        params.put("correo", datos == null ? null : datos.getCorreo());
        params.put("contrasena", datos == null ? null : datos.getContrasena());

        Respuesta respuesta = gateway.enviar("usuarios", "registrar", params);
        if (!respuesta.isExitosa()) {
            return RespuestaHttp.error(respuesta);
        }
        return ResponseEntity.status(201).body(RespuestaHttp.resultadoComoMapa(respuesta));
    }
}
