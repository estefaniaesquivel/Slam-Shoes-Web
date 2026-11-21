package com.slamshoes.broker.http;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.RegistroServicios;

/**
 * Reenvío HTTP transparente (proxy): {@code GET /api/gateway/{servicio}/{ruta...}} se convierte en la misma
 * petición hacia {@code {url del proveedor}{rutaBase}/{ruta...}}, y el código, el tipo y el cuerpo que devuelve el
 * proveedor se entregan sin cambios al cliente (Vue 3 o Bruno).
 *
 * Es el segundo modo de uso del Broker (el primero es la envoltura Solicitud/Respuesta de
 * {@link NucleoBroker#solicitar(Map, String)}): para consultas REST normales no hace falta armar una Solicitud.
 * El cliente solo conoce el NOMBRE del servicio; la URL real sale del directorio dinámico, así que si el
 * proveedor cambia de dirección (otro despliegue de Render) el cliente no se entera.
 *
 * Seguridad: solo reenvía a proveedores que se registraron (nunca a una URL elegida por el cliente), solo
 * métodos GET/POST/PUT/PATCH/DELETE, rechaza rutas con {@code ..} o separadores codificados y reenvía únicamente las
 * cabeceras Content-Type y Accept.
 *
 * REFACTOR: Extract Class
 * RAZÓN: reenviar una petición REST tiene reglas propias (validar la ruta, elegir cabeceras, traducir fallos a 502/504)
 * que no tienen que ver con registrar proveedores ni con el patrón Solicitud/Respuesta. Aislarlas en una clase evita
 * que {@link NucleoBroker} crezca y permite probarlas por separado.
 */
public class ProxyHttp {

    private static final Set<String> METODOS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final Pattern RUTA_SEGURA = Pattern.compile("(/[A-Za-z0-9._~!$&'()*+,;=:@%-]+)*/?");

    private final RegistroServicios registro;
    private final Bitacora bitacora;
    private final int msConexion;
    private final int msLectura;

    public ProxyHttp(RegistroServicios registro, Bitacora bitacora, int msConexion, int msLectura) {
        this.registro = registro;
        this.bitacora = bitacora;
        this.msConexion = msConexion;
        this.msLectura = msLectura;
    }

    /**
     * Reenvía la petición al proveedor del servicio.
     *
     * @param servicio     nombre del servicio registrado (por ejemplo "productos")
     * @param metodo       método HTTP original
     * @param resto        ruta que sigue al nombre del servicio, con "/" inicial o vacía
     * @param query        parámetros después de "?" (sin el "?"), o null
     * @param cuerpo       cuerpo original (null si no hubo)
     * @param tipoContenido cabecera Content-Type original (puede ser null)
     * @param aceptar      cabecera Accept original (puede ser null)
     * @return lo que respondió el proveedor, o un 4xx/5xx JSON si no se pudo reenviar
     */
    public MensajeHttp reenviar(String servicio, String metodo, String resto, String query, String cuerpo,
                                String tipoContenido, String aceptar) {
        String metodoNormal = metodo == null ? "" : metodo.toUpperCase();
        if (!METODOS.contains(metodoNormal)) {
            return error(405, EstadoRespuesta.SOLICITUD_INVALIDA, "Metodo no permitido: " + metodo);
        }
        String ruta = resto == null ? "" : resto;
        String rutaMinuscula = ruta.toLowerCase();
        if (!RUTA_SEGURA.matcher(ruta).matches() || ruta.contains("..") || rutaMinuscula.contains("%2e")
                || rutaMinuscula.contains("%2f") || rutaMinuscula.contains("%5c") || rutaMinuscula.contains("%00")) {
            return error(400, EstadoRespuesta.SOLICITUD_INVALIDA, "Ruta no permitida");
        }

        Optional<ProveedorServicio> encontrado = registro.buscar(servicio);
        if (encontrado.isEmpty()) {
            Consola.broker("Proxy: no hay proveedor para '" + servicio + "'");
            return error(404, EstadoRespuesta.SERVICIO_NO_ENCONTRADO,
                    "No existe un proveedor registrado para el servicio '" + servicio + "'");
        }
        if (!(encontrado.get() instanceof ProveedorHttp proveedor)) {
            return error(501, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                    "El servicio '" + servicio + "' es local y no se puede alcanzar por el proxy HTTP");
        }

        URI destino;
        try {
            destino = URI.create(proveedor.getUrl() + proveedor.getRutaBase() + ruta
                    + (query == null || query.isBlank() ? "" : "?" + query));
        } catch (IllegalArgumentException e) {
            return error(400, EstadoRespuesta.SOLICITUD_INVALIDA, "Parametros de la URL invalidos");
        }

        Map<String, String> cabeceras = new LinkedHashMap<>();
        if (tipoContenido != null && !tipoContenido.isBlank()) {
            cabeceras.put("Content-Type", tipoContenido);
        }
        cabeceras.put("Accept", aceptar == null || aceptar.isBlank() ? "application/json" : aceptar);
        if (proveedor.getToken() != null) {
            cabeceras.put(RutasBroker.CABECERA_TOKEN, proveedor.getToken());
        }

        long inicio = System.nanoTime();
        try {
            Consola.broker("Proxy " + metodoNormal + " /" + servicio + ruta + " -> " + destino);
            MensajeHttp respuesta = TransporteHttp.enviar(metodoNormal, destino, cuerpo, cabeceras, msConexion, msLectura);
            if (respuesta.esTransitorio()) {
                proveedor.registrarFallo();
            } else {
                proveedor.tocar();
            }
            anotar(servicio, metodoNormal, ruta, respuesta.codigo() < 500 ? EstadoRespuesta.EXITO : EstadoRespuesta.ERROR_PROVEEDOR,
                    inicio, "HTTP " + respuesta.codigo());
            return respuesta;
        } catch (IOException e) {
            proveedor.registrarFallo();
            boolean tiempo = TransporteHttp.esTiempoAgotado(e);
            anotar(servicio, metodoNormal, ruta,
                    tiempo ? EstadoRespuesta.TIEMPO_AGOTADO : EstadoRespuesta.PROVEEDOR_NO_DISPONIBLE, inicio, e.getClass().getSimpleName());
            return tiempo
                    ? error(504, EstadoRespuesta.TIEMPO_AGOTADO, "El proveedor '" + servicio + "' no respondio a tiempo")
                    : error(502, EstadoRespuesta.PROVEEDOR_NO_DISPONIBLE, "No se pudo contactar al proveedor '" + servicio + "'");
        }
    }

    private void anotar(String servicio, String metodo, String ruta, EstadoRespuesta estado, long inicioNanos, String detalle) {
        bitacora.registrar("proxy", servicio, metodo + " " + (ruta.isEmpty() ? "/" : ruta), estado,
                (System.nanoTime() - inicioNanos) / 1_000_000, detalle);
    }

    private static MensajeHttp error(int codigo, EstadoRespuesta estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", estado.name());
        cuerpo.put("error", mensaje);
        return new MensajeHttp(codigo, "application/json", Json.escribir(cuerpo));
    }
}
