package com.slamshoes.broker.http;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * Lado PROVEEDOR de la comunicación HTTP: recibe la solicitud que el Broker reenvía a
 * {@code POST /api/proveedor/{servicio}}, ejecuta la operación en el {@link ProveedorServicio} correspondiente y
 * devuelve la {@link Respuesta}. Nunca lanza excepciones: cualquier fallo se convierte en una respuesta con su estado.
 *
 * Una misma aplicación puede alojar varios proveedores (productos, usuarios, pedidos); cada uno tiene su propio
 * detector de duplicados. Reemplaza a la mitad "servidor" de la antigua clase {@code ProveedorServidor} (la mitad
 * que abría un {@code ServerSocket}); la otra mitad, el registro periódico, es {@link RegistroAutomatico}.
 *
 * REFACTOR: Extract Class + Remove Dead Code
 * RAZÓN: {@code ProveedorServidor} mezclaba escuchar en un puerto, atender solicitudes y registrarse en el Broker.
 * Con HTTP, Spring ya escucha y enruta, así que solo queda la responsabilidad de atender la solicitud, que vive aquí,
 * sin sockets ni hilos propios, y con una única razón para cambiar.
 */
public class ExposicionProveedores {

    private final Map<String, ProveedorServicio> proveedores = new ConcurrentHashMap<>();
    private final Map<String, CacheDuplicados<Respuesta>> duplicados = new ConcurrentHashMap<>();
    private final String token;

    /**
     * @param proveedores los proveedores de negocio que esta aplicación aloja
     * @param token       secreto compartido con el Broker (null o vacío = sin autenticación)
     */
    public ExposicionProveedores(List<? extends ProveedorServicio> proveedores, String token) {
        for (ProveedorServicio p : proveedores) {
            this.proveedores.put(p.getNombreServicio(), p);
            this.duplicados.put(p.getNombreServicio(), new CacheDuplicados<>(1000));
        }
        this.token = token == null || token.isBlank() ? null : token;
    }

    /** Proveedores que expone esta aplicación (para el auto-registro). */
    public List<ProveedorServicio> getProveedores() {
        return List.copyOf(proveedores.values());
    }

    /** true si no hay secreto configurado o el enviado coincide (comparación en tiempo constante). */
    public boolean autorizado(String enviado) {
        if (token == null) {
            return true;
        }
        return enviado != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                enviado.getBytes(StandardCharsets.UTF_8));
    }

    /** Estado ligero para {@code GET /api/proveedor/health}. */
    public Map<String, Object> salud() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("componente", "proveedores");
        m.put("servicios", new TreeSet<>(proveedores.keySet()));
        return m;
    }

    /**
     * Atiende una solicitud recibida por HTTP.
     *
     * @param servicio nombre del servicio indicado en la URL
     * @param json     cuerpo JSON recibido (la {@link Solicitud} serializada por el Broker)
     * @return siempre una Respuesta: de éxito, o de error con su estado
     */
    public Respuesta atender(String servicio, String json) {
        ProveedorServicio proveedor = proveedores.get(servicio);
        if (proveedor == null) {
            Consola.proveedor(String.valueOf(servicio), "Servicio desconocido en esta aplicacion");
            return Respuesta.error("sin-id", EstadoRespuesta.SERVICIO_NO_ENCONTRADO,
                    "Esta aplicacion no aloja el servicio '" + servicio + "'");
        }

        Solicitud solicitud;
        try {
            solicitud = Marshalling.unmarshalSolicitud(Marshalling.leerMensaje(json));        // unmarshalling
        } catch (IllegalArgumentException e) {
            Consola.proveedor(servicio, "Mensaje invalido: " + e.getMessage());
            return Respuesta.error("sin-id", EstadoRespuesta.SOLICITUD_INVALIDA, "Solicitud mal formada: " + e.getMessage());
        }
        if (!servicio.equals(solicitud.getServicio())) {
            return Respuesta.error(solicitud.getIdSolicitud(), EstadoRespuesta.SOLICITUD_INVALIDA,
                    "La solicitud es para '" + solicitud.getServicio() + "' pero llego a '" + servicio + "'");
        }

        Consola.proveedor(servicio, "Solicitud " + solicitud.getIdCorto() + " " + solicitud.getOperacion()
                + " " + solicitud.getParametrosParaLog());
        CacheDuplicados.Resultado<Respuesta> resultado = duplicados.get(servicio).ejecutar(
                solicitud.getIdSolicitud(), () -> ejecutarSeguro(proveedor, solicitud), Respuesta::isExitosa);
        if (resultado.duplicada) {
            Consola.proveedor(servicio, "Solicitud " + solicitud.getIdCorto()
                    + " DUPLICADA: no se vuelve a ejecutar, se devuelve la respuesta anterior");
        }
        return resultado.valor;
    }

    /** Ejecuta la operación; cualquier excepción se convierte en una respuesta ERROR_PROVEEDOR. */
    private Respuesta ejecutarSeguro(ProveedorServicio proveedor, Solicitud solicitud) {
        String id = solicitud.getIdSolicitud();
        String servicio = proveedor.getNombreServicio();
        try {
            Respuesta respuesta = proveedor.ejecutar(solicitud);
            if (respuesta == null) {
                return Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR, "El proveedor devolvio una respuesta nula");
            }
            if (!id.equals(respuesta.getIdSolicitud())) {
                return Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR,
                        "El proveedor devolvio una respuesta con otro idSolicitud");
            }
            return respuesta;
        } catch (Exception e) {
            Consola.proveedor(servicio, "EXCEPCION en " + solicitud.getOperacion() + ": " + e);
            return Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR,
                    "El proveedor fallo: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
