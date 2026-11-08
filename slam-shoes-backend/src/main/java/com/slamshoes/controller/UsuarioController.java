package com.slamshoes.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.dto.RegistroDTO;
import com.slamshoes.model.Usuario;
import com.slamshoes.repository.UsuarioRepository;
import com.slamshoes.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173") // Permite la conexión con tu frontend de Vue
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Datos del usuario que se devuelven al frontend (nunca el hash de la contraseña)
    private Map<String, Object> usuarioPublico(Usuario u) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("idUsuario", u.getIdUsuario());
        datos.put("nombre", u.getNombre());
        datos.put("correo", u.getCorreo());
        datos.put("rol", u.getRol());
        datos.put("fechaRegistro", u.getFechaRegistro());
        return datos;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String correo = credentials.get("correo");
        String contrasena = credentials.get("contrasena");

        // Buscar usuario por correo
        Usuario usuario = usuarioRepository.findByCorreo(correo);
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("error", "El correo no está registrado"));
        }

        // Validar contraseña encriptada con BCrypt
        boolean passwordMatch = passwordEncoder.matches(contrasena, usuario.getContrasenaHash());
        if (!passwordMatch) {
            return ResponseEntity.status(401).body(Map.of("error", "Contraseña incorrecta"));
        }

        // Login exitoso: se devuelve el usuario con su rol (CLIENTE o ADMIN) para que el frontend redirija
        return ResponseEntity.ok(usuarioPublico(usuario));
    }

    // Registro público de clientes nuevos. El rol NO viene del cliente: siempre se asigna CLIENTE.
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroDTO datos) {
        if (datos.getNombre() == null || datos.getNombre().isBlank()
                || datos.getCorreo() == null || datos.getCorreo().isBlank()
                || datos.getContrasena() == null || datos.getContrasena().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nombre, correo y contraseña (mínimo 6 caracteres) son obligatorios"));
        }

        String correo = datos.getCorreo().trim();
        if (usuarioRepository.findByCorreo(correo) != null) {
            return ResponseEntity.status(409).body(Map.of("error", "Ese correo ya está registrado"));
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(datos.getNombre().trim());
        nuevo.setCorreo(correo);
        nuevo.setContrasenaHash(datos.getContrasena()); // el servicio la encripta con BCrypt

        Usuario guardado = usuarioService.registrarUsuario(nuevo); // fuerza rol = CLIENTE
        return ResponseEntity.status(201).body(usuarioPublico(guardado));
    }
}
