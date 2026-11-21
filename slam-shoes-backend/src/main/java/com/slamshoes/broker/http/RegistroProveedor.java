package com.slamshoes.broker.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Datos con los que un proveedor se da de alta en el Broker (cuerpo JSON de {@code POST /api/broker/register}).
 *
 * <pre>
 * { "servicio": "catalogo",
 *   "url": "https://slam-catalogo.onrender.com",      // URL pública/interna del proveedor
 *   "rutaBase": "/api/productos",                       // opcional: raíz REST para el proxy /api/gateway
 *   "operaciones": ["listarProductos", "..."],          // opcional: si falta, no se valida la operación
 *   "reemplazar": false }                               // opcional: sustituir a otro proveedor del servicio
 * </pre>
 *
 * REFACTOR: Introduce Parameter Object
 * RAZÓN: antes el Broker recibía un {@code Map} suelto y leía a mano "host", "puerto" y "operaciones" con
 * comprobaciones repetidas dentro de un método de 40 líneas. Ahora el mensaje se convierte UNA vez en este
 * objeto ya validado: el resto del código recibe datos correctos (o una excepción clara) y ya no repite validaciones.
 *
 * REFACTOR: Replace Primitive with Object
 * RAZÓN: el destino del proveedor eran dos primitivos sueltos (host + puerto) que había que mantener
 * sincronizados; ahora es un {@link URI} validado, que además admite HTTPS y el puerto asignado por Render.
 *
 * @param servicio    nombre del servicio (letras, números, guion y guion bajo)
 * @param url         URL base del proveedor, sin barra final, solo http/https
 * @param rutaBase    ruta REST base para el proxy (puede ser vacía)
 * @param operaciones operaciones que ofrece (vacío = sin catálogo declarado)
 * @param reemplazar  si es true sustituye a otro proveedor ya registrado con ese nombre
 */
public record RegistroProveedor(String servicio, URI url, String rutaBase, Set<String> operaciones,
                                boolean reemplazar) {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[A-Za-z0-9_-]{1,40}");
    private static final Pattern RUTA_VALIDA = Pattern.compile("(/[A-Za-z0-9._~-]+)*");

    /**
     * Construye el registro a partir del JSON ya convertido a mapa.
     *
     * @throws IllegalArgumentException con un mensaje legible si falta algo o es inválido
     */
    public static RegistroProveedor desdeMapa(Map<String, Object> json) {
        if (json == null) {
            throw new IllegalArgumentException("El cuerpo del registro es obligatorio");
        }
        String servicio = texto(json.get("servicio"));
        if (servicio == null || !NOMBRE_VALIDO.matcher(servicio).matches()) {
            throw new IllegalArgumentException(
                    "El campo 'servicio' es obligatorio (1 a 40 letras, numeros, guion o guion bajo)");
        }
        URI url = urlBase(texto(json.get("url")));

        String ruta = texto(json.get("rutaBase"));
        if (ruta == null) {
            ruta = "";
        } else if (!ruta.startsWith("/")) {
            ruta = "/" + ruta;
        }
        while (ruta.endsWith("/")) {
            ruta = ruta.substring(0, ruta.length() - 1);
        }
        if (!RUTA_VALIDA.matcher(ruta).matches() || ruta.contains("..")) {
            throw new IllegalArgumentException("El campo 'rutaBase' no es una ruta valida: '" + ruta + "'");
        }

        Set<String> operaciones = new TreeSet<>();
        Object crudas = json.get("operaciones");
        if (crudas != null) {
            if (!(crudas instanceof List<?> lista)) {
                throw new IllegalArgumentException("El campo 'operaciones' debe ser una lista de textos");
            }
            for (Object o : lista) {
                if (o != null && !o.toString().isBlank()) {
                    operaciones.add(o.toString().trim());
                }
            }
        }
        return new RegistroProveedor(servicio, url, ruta, operaciones, Boolean.TRUE.equals(json.get("reemplazar")));
    }

    /** Valida y normaliza una URL base: http/https, con servidor, sin credenciales, parámetros ni barra final. */
    static URI urlBase(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El campo 'url' es obligatorio (por ejemplo https://mi-servicio.onrender.com)");
        }
        URI uri;
        try {
            uri = new URI(texto.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("La 'url' no es valida: " + e.getReason());
        }
        String esquema = uri.getScheme();
        if (esquema == null || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("La 'url' debe empezar con http:// o https://");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "La 'url' debe ser solo el servidor (sin usuario, parametros ni #fragmento)");
        }
        String ruta = uri.getPath() == null ? "" : uri.getPath();
        while (ruta.endsWith("/")) {
            ruta = ruta.substring(0, ruta.length() - 1);
        }
        try {
            return new URI(esquema.toLowerCase(), null, uri.getHost(), uri.getPort(), ruta.isEmpty() ? null : ruta, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("La 'url' no es valida: " + e.getReason());
        }
    }

    /** Representación JSON-friendly (la que viaja en el cuerpo del POST de registro). */
    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("servicio", servicio);
        m.put("url", url.toString());
        m.put("rutaBase", rutaBase);
        m.put("operaciones", new ArrayList<>(operaciones));
        m.put("reemplazar", reemplazar);
        return m;
    }

    private static String texto(Object valor) {
        return valor instanceof String s && !s.isBlank() ? s.trim() : null;
    }
}
