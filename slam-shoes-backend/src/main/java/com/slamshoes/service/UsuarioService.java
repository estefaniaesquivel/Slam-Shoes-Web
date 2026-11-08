package com.slamshoes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.slamshoes.model.Usuario;
import com.slamshoes.repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Método para Registrar
    // Registro público: SIEMPRE crea un CLIENTE, sin importar el rol que venga en la petición
    public Usuario registrarUsuario(Usuario usuario) {
        usuario.setRol("CLIENTE");
        usuario.setFechaRegistro(java.time.LocalDateTime.now());
        // Encriptamos la contraseña en texto plano antes de guardarla
        String hashedPassword = passwordEncoder.encode(usuario.getContrasenaHash());
        usuario.setContrasenaHash(hashedPassword);
        
        return usuarioRepository.save(usuario);
    }

    // Método para el Login
    public boolean validarLogin(String correo, String contrasenaPlana) {
        Usuario usuario = usuarioRepository.findByCorreo(correo);
        if (usuario == null) {
            return false; // El usuario no existe
        }
        
        // Compara la contraseña escrita con el hash almacenado en Supabase
        return passwordEncoder.matches(contrasenaPlana, usuario.getContrasenaHash());
    }
}