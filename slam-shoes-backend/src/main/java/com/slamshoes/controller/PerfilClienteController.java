package com.slamshoes.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slamshoes.model.PerfilCliente;
import com.slamshoes.repository.PerfilClienteRepository;
import com.slamshoes.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/perfiles")
@CrossOrigin(origins = "*")
public class PerfilClienteController {

    private static final Set<String> METODOS_PAGO = Set.of("Tarjeta de débito", "Tarjeta de crédito");

    @Autowired
    private PerfilClienteRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Perfil del cliente; si aún no lo ha llenado devuelve uno vacío
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<PerfilCliente> obtener(@PathVariable Long idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            return ResponseEntity.notFound().build();
        }
        PerfilCliente perfil = perfilRepository.findByIdUsuario(idUsuario).orElseGet(() -> {
            PerfilCliente vacio = new PerfilCliente();
            vacio.setIdUsuario(idUsuario);
            return vacio;
        });
        return ResponseEntity.ok(perfil);
    }

    // Crea o actualiza el perfil (dirección, método de pago como texto y talla preferida)
    @PutMapping("/usuario/{idUsuario}")
    public ResponseEntity<?> guardar(@PathVariable Long idUsuario, @RequestBody PerfilCliente datos) {
        if (!usuarioRepository.existsById(idUsuario)) {
            return ResponseEntity.notFound().build();
        }
        String metodo = datos.getMetodoPago();
        if (metodo != null && !metodo.isBlank() && !METODOS_PAGO.contains(metodo)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Método de pago no válido"));
        }

        PerfilCliente perfil = perfilRepository.findByIdUsuario(idUsuario).orElseGet(PerfilCliente::new);
        perfil.setIdUsuario(idUsuario);
        perfil.setDireccion(datos.getDireccion());
        perfil.setMetodoPago(metodo == null || metodo.isBlank() ? null : metodo);
        perfil.setTallaPreferida(datos.getTallaPreferida() == null || datos.getTallaPreferida().isBlank()
                ? null : datos.getTallaPreferida());
        return ResponseEntity.ok(perfilRepository.save(perfil));
    }
}
