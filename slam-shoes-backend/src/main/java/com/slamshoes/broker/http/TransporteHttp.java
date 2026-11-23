package com.slamshoes.broker.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Único punto del sistema que sabe "cómo se habla con otro proceso": peticiones HTTP/HTTPS estándar
 * (cliente nativo {@code java.net.http.HttpClient}, el mismo motor que usa RestClient de Spring).
 *
 * Con tiempos máximos de conexión y de lectura (timeouts). Reemplaza a la antigua clase {@code Canal},
 * que abría un {@code Socket} TCP crudo hacia un puerto propio.
 *
 * REFACTOR: Substitute Algorithm
 * RAZÓN: el algoritmo anterior (abrir Socket, escribir una línea, leer una línea) necesitaba puertos TCP
 * personalizados, que Render bloquea: solo deja pasar HTTP/HTTPS (80/443) por su proxy inverso. El nuevo
 * algoritmo hace lo mismo (enviar un mensaje y esperar la respuesta, con timeouts) pero sobre HTTP, de modo
 * que el despliegue funciona y el resto del Broker no cambia porque la firma de intercambio es equivalente.
 */
public final class TransporteHttp {

    private TransporteHttp() {
    }

    /**
     * Envía una petición y espera la respuesta completa.
     *
     * @param metodo      GET, POST, PUT, DELETE o PATCH
     * @param uri         destino absoluto (http o https)
     * @param cuerpo      texto a enviar (puede ser null en GET/DELETE)
     * @param cabeceras   cabeceras adicionales (puede ser null); Content-Type se incluye aquí
     * @param msConexion  tiempo máximo para establecer la conexión
     * @param msLectura   tiempo máximo esperando la respuesta
     * @return la respuesta, aunque su código sea 4xx/5xx (decidir qué hacer es trabajo del llamador)
     * @throws HttpTimeoutException si no conectó o no respondió a tiempo
     * @throws IOException          si la conexión se rechazó o se cortó
     */
    // REFACTOR: Change Function Declaration
    // RAZON: antes {@code Canal.intercambiar(host, puerto, linea, msConexion, msLectura)} solo sabia enviar UNA
    // linea de texto a un host:puerto TCP. Ahora la firma recibe metodo, URI, cuerpo y cabeceras y devuelve un
    // MensajeHttp (codigo + tipo + cuerpo), que es lo que HTTP necesita para decidir reintentos y para que el
    // proxy reenvie el codigo del proveedor sin perderlo.
    public static MensajeHttp enviar(String metodo, URI uri, String cuerpo, Map<String, String> cabeceras,
                                     int msConexion, int msLectura) throws IOException {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(Math.max(1, msConexion + msLectura)));
        if (cabeceras != null) {
            cabeceras.forEach((nombre, valor) -> {
                if (valor != null) {
                    peticion.header(nombre, valor);
                }
            });
        }
        boolean llevaCuerpo = cuerpo != null && !metodo.equals("GET") && !metodo.equals("DELETE");
        peticion.method(metodo, llevaCuerpo
                ? HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8)
                : HttpRequest.BodyPublishers.noBody());

        try {
            HttpResponse<String> respuesta = clienteConConexion(msConexion)
                    .send(peticion.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new MensajeHttp(respuesta.statusCode(),
                    respuesta.headers().firstValue("Content-Type").orElse(null), respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Peticion interrumpida", e);
        }
    }

    /** POST de un JSON; atajo muy usado por el Broker, los proveedores y el cliente. */
    // REFACTOR: Extract Function
    // RAZON: armar el POST con Content-Type y Accept JSON se repetia en cada llamador (cliente, Broker y auto-
    // registro). Un solo atajo garantiza las mismas cabeceras en todos y deja a cada llamador solo con su logica.
    public static MensajeHttp postJson(URI uri, String json, Map<String, String> cabeceras,
                                       int msConexion, int msLectura) throws IOException {
        Map<String, String> todas = new LinkedHashMap<>();
        todas.put("Content-Type", "application/json; charset=UTF-8");
        todas.put("Accept", "application/json");
        if (cabeceras != null) {
            todas.putAll(cabeceras);
        }
        return enviar("POST", uri, json, todas, msConexion, msLectura);
    }

    /** true si la excepción significa "tardó demasiado" (y no "no se pudo conectar"). */
    // REFACTOR: Extract Function
    // RAZON: antes cada clase comprobaba {@code e instanceof SocketTimeoutException} por su cuenta
    // (ProveedorRemoto y ClienteSocket). Al cambiar el tipo de excepcion de HTTP ({@code HttpTimeoutException})
    // habria que corregirlo en varios lugares; ahora la pregunta 'tardo demasiado o no se pudo conectar' se
    // responde en un unico metodo.
    public static boolean esTiempoAgotado(IOException e) {
        return e instanceof HttpTimeoutException;
    }

    // El timeout de conexión se fija al construir el cliente; se reutiliza uno por cada valor distinto.
    private static final ConcurrentHashMap<Integer, HttpClient> POR_CONEXION = new ConcurrentHashMap<>();

    // REFACTOR: Substitute Algorithm
    // RAZON: {@code Canal} abria un Socket nuevo por cada mensaje y lo cerraba. Aqui se reutiliza un HttpClient
    // por valor de timeout de conexion, con lo que se aprovechan las conexiones persistentes y no se crea un
    // cliente por peticion; el resultado para quien llama es el mismo.
    private static HttpClient clienteConConexion(int msConexion) {
        return POR_CONEXION.computeIfAbsent(Math.max(1, msConexion), ms -> HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(ms))
                .build());
    }
}
