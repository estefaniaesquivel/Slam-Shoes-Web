package com.slamshoes.proveedores;

import static com.slamshoes.providers.ProveedorUtil.mapa;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.model.Usuario;
import com.slamshoes.repository.UsuarioRepository;
import com.slamshoes.service.UsuarioService;

/**
 * Proveedor REAL del servicio "usuarios" (con base de datos): autenticacion y registro.
 *
 * Reglas que viven aqui y no en el controlador: las contrasenas se comparan con BCrypt, el rol de
 * un registro publico SIEMPRE es CLIENTE y NUNCA se devuelve el hash de la contrasena.
 *
 * Operaciones:
 *  - autenticar {correo, contrasena}          -> datos publicos del usuario (NO_AUTORIZADO si no coincide)
 *  - registrar  {nombre, correo, contrasena}  -> usuario creado (CONFLICTO si el correo ya existe)
 */
@Component
public class ServicioUsuariosJpa implements ProveedorServicio {

    private static final String NOMBRE_CLASE = "ServicioUsuariosJpa";
    private static final Set<String> OPERACIONES = Set.of("autenticar", "registrar");

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioService usuarioService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Override
    public String getNombreServicio() {
        return "usuarios";
    }

    @Override
    public Set<String> getOperacionesSoportadas() {
        return OPERACIONES;
    }

    @Override
    public Respuesta ejecutar(Solicitud solicitud) {
        String id = solicitud.getIdSolicitud();
        switch (solicitud.getOperacion()) {
            case "autenticar":
                return autenticar(id, solicitud.getParametros());
            case "registrar":
                return registrar(id, solicitud.getParametros());
            default:
                return Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                        "Operacion no soportada: " + solicitud.getOperacion());
        }
    }

    private Respuesta autenticar(String id, Map<String, Object> parametros) {
        String correo = texto(parametros.get("correo"));
        String contrasena = parametros.get("contrasena") == null ? "" : parametros.get("contrasena").toString();
        if (correo.isEmpty() || contrasena.isEmpty()) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, "Escribe tu correo y tu contraseña");
        }

        Usuario usuario = usuarioRepository.findByCorreo(correo);
        // Mismo mensaje si falla el correo o la contrasena: no se revela cual de los dos fue
        if (usuario == null || usuario.getContrasenaHash() == null
                || !passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
            return Respuesta.error(id, EstadoRespuesta.NO_AUTORIZADO, "Correo o contraseña incorrectos");
        }
        return Respuesta.exito(id, usuarioPublico(usuario));
    }

    private Respuesta registrar(String id, Map<String, Object> parametros) {
        String nombre = texto(parametros.get("nombre"));
        String correo = texto(parametros.get("correo"));
        String contrasena = parametros.get("contrasena") == null ? "" : parametros.get("contrasena").toString();

        if (nombre.isEmpty() || correo.isEmpty() || contrasena.length() < 6) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS,
                    "Nombre, correo y contraseña (mínimo 6 caracteres) son obligatorios");
        }
        if (!correo.contains("@")) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, "El correo '" + correo + "' no es válido");
        }
        if (usuarioRepository.findByCorreo(correo) != null) {
            return Respuesta.error(id, EstadoRespuesta.CONFLICTO, "Ese correo ya está registrado");
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre);
        nuevo.setCorreo(correo);
        nuevo.setContrasenaHash(contrasena); // el servicio la encripta con BCrypt y fuerza rol = CLIENTE
        Usuario guardado = usuarioService.registrarUsuario(nuevo);
        return Respuesta.exito(id, usuarioPublico(guardado));
    }

    /** Datos que pueden salir del proveedor: nunca el hash de la contrasena. */
    private Map<String, Object> usuarioPublico(Usuario u) {
        return mapa("atendidoPor", NOMBRE_CLASE, "idUsuario", u.getIdUsuario(), "nombre", u.getNombre(),
                "correo", u.getCorreo(), "rol", u.getRol(), "fechaRegistro", u.getFechaRegistro());
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }
}
