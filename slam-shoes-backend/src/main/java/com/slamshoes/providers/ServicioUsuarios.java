package com.slamshoes.providers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import static com.slamshoes.providers.ProveedorUtil.mapa;
import static com.slamshoes.providers.ProveedorUtil.obtenerTexto;

/**
 * Proveedor del servicio "usuarios": autenticación y registro.
 *
 * NOTA (Nivel 1): usa usuarios de ejemplo en memoria. Las contraseñas se guardan
 * como hash (SHA-256, solo para la demo) y nunca se devuelven. En la versión
 * integrada se usa UsuarioRepository y BCrypt, igual que UsuarioService.
 *
 * Cuentas de ejemplo: demo@slamshoes.com / demo123 (CLIENTE) y
 * admin@slamshoes.com / admin123 (ADMIN).
 */
public class ServicioUsuarios implements ProveedorServicio {

    private static final Set<String> OPERACIONES = Set.of("autenticar", "registrar");

    private static class UsuarioDemo {
        final long idUsuario;
        final String nombre;
        final String correo;
        final String hash;
        final String rol;

        UsuarioDemo(long idUsuario, String nombre, String correo, String hash, String rol) {
            this.idUsuario = idUsuario;
            this.nombre = nombre;
            this.correo = correo;
            this.hash = hash;
            this.rol = rol;
        }
    }

    private final Map<String, UsuarioDemo> usuariosPorCorreo = new ConcurrentHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(3);

    public ServicioUsuarios() {
        agregar(new UsuarioDemo(1, "Cliente Demo", "demo@slamshoes.com", hash("demo123"), "CLIENTE"));
        agregar(new UsuarioDemo(2, "Admin Demo", "admin@slamshoes.com", hash("admin123"), "ADMIN"));
    }

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
        try {
            return switch (solicitud.getOperacion()) {
                case "autenticar" -> Respuesta.exito(id, autenticar(solicitud.getParametros()));
                case "registrar" -> Respuesta.exito(id, registrar(solicitud.getParametros()));
                default -> Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                        "Operacion no soportada: " + solicitud.getOperacion());
            };
        } catch (IllegalArgumentException e) {
            return Respuesta.error(id, EstadoRespuesta.PARAMETROS_INVALIDOS, e.getMessage());
        }
    }

    private Map<String, Object> autenticar(Map<String, Object> parametros) {
        String correo = obtenerTexto(parametros, "correo");
        String contrasena = obtenerTexto(parametros, "contrasena");

        UsuarioDemo usuario = usuariosPorCorreo.get(correo.toLowerCase());
        // Mismo mensaje si falla el correo o la contrasena, para no revelar cual fue
        if (usuario == null || !coincide(usuario.hash, hash(contrasena))) {
            throw new IllegalArgumentException("Correo o contrasena incorrectos");
        }
        // Solo datos seguros: nunca el hash
        return mapa("atendidoPor", getClass().getSimpleName(), "idUsuario", usuario.idUsuario,
                "nombre", usuario.nombre, "correo", usuario.correo, "rol", usuario.rol);
    }

    private Map<String, Object> registrar(Map<String, Object> parametros) {
        String nombre = obtenerTexto(parametros, "nombre");
        String correo = obtenerTexto(parametros, "correo");
        String contrasena = obtenerTexto(parametros, "contrasena");

        if (!correo.contains("@")) {
            throw new IllegalArgumentException("El correo '" + correo + "' no es valido");
        }
        if (contrasena.length() < 6) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 6 caracteres");
        }
        UsuarioDemo nuevo = new UsuarioDemo(siguienteId.getAndIncrement(), nombre, correo, hash(contrasena), "CLIENTE");
        if (usuariosPorCorreo.putIfAbsent(correo.toLowerCase(), nuevo) != null) {
            throw new IllegalArgumentException("El correo '" + correo + "' ya esta registrado");
        }
        return mapa("atendidoPor", getClass().getSimpleName(), "idUsuario", nuevo.idUsuario,
                "nombre", nuevo.nombre, "correo", nuevo.correo, "rol", nuevo.rol);
    }

    private void agregar(UsuarioDemo usuario) {
        usuariosPorCorreo.put(usuario.correo.toLowerCase(), usuario);
    }

    private static boolean coincide(String hashA, String hashB) {
        return MessageDigest.isEqual(hashA.getBytes(StandardCharsets.UTF_8), hashB.getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest(texto.getBytes(StandardCharsets.UTF_8))) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
