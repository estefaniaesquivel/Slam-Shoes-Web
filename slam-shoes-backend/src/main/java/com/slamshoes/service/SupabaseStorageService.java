package com.slamshoes.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.slamshoes.exception.ValidacionException;

// REFACTOR: Extract Interface
// RAZON: Antes esta clase era la única dependencia concreta de Storage, con tres campos @Value privados y un HttpClient
// creado dentro. Ahora implementa IStorageService y el HttpClient entra por constructor, de modo que
// SupabaseStorageServiceTest lo reemplaza por un mock de HttpClient sin tocar la red.
/**
 * Implementación de {@link IStorageService} respaldada por la API REST de Supabase Storage.
 * Las carpetas de Storage se crean automáticamente al subir un objeto con ruta {@code carpeta/archivo}.
 */
@Service
public class SupabaseStorageService implements IStorageService {

    private static final Logger LOG = Logger.getLogger(SupabaseStorageService.class.getName());

    private final String url;
    private final String bucket;
    private final String serviceKey;
    private final HttpClient http;

    /**
     * Constructor principal usado por Spring con la configuración de propiedades.
     *
     * @param url URL base del proyecto Supabase
     * @param bucket nombre del bucket de almacenamiento
     * @param serviceKey llave de servicio para autenticación Bearer
     */
    @Autowired
    public SupabaseStorageService(
            @Value("${supabase.url:https://ckparlqrfkzofsvskmbw.supabase.co}") String url,
            @Value("${supabase.bucket:slam-shoes-storage}") String bucket,
            @Value("${supabase.service-key:}") String serviceKey) {
        this(url, bucket, serviceKey, HttpClient.newHttpClient());
    }

    // REFACTOR: Remove Setting Method
    // RAZON: Antes url, bucket y serviceKey eran campos @Value privados mutables y http se creaba en la declaración
    // (private final HttpClient http = HttpClient.newHttpClient()). Ahora los cuatro son campos final asignados por
    // constructor; este constructor existe para entregar un HttpClient simulado en las pruebas.
    /**
     * Constructor auxiliar para inyectar un {@link HttpClient} en pruebas unitarias.
     *
     * @param url URL base del proyecto Supabase
     * @param bucket nombre del bucket de almacenamiento
     * @param serviceKey llave de servicio para autenticación Bearer
     * @param http cliente HTTP a utilizar
     */
    public SupabaseStorageService(String url, String bucket, String serviceKey, HttpClient http) {
        this.url = url;
        this.bucket = bucket;
        this.serviceKey = serviceKey;
        this.http = http;
    }

    @Override
    public boolean estaConfigurado() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    @Override
    public String subir(String carpeta, String archivo, byte[] contenido, String contentType)
            throws IOException, InterruptedException {
        String ruta = carpeta + "/" + archivo;
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(url + "/storage/v1/object/" + bucket + "/" + ruta))
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(contenido))
                .build();

        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 201) {
            throw new IOException("Supabase respondió " + respuesta.statusCode() + ": " + respuesta.body());
        }
        return urlPublica(ruta);
    }

    // REFACTOR: Replace Error Code with Exception
    // RAZON: Antes un archivo que no era WebP lanzaba IllegalArgumentException, que cada controlador capturaba para devolver 400.
    // Ahora lanza ValidacionException, que GlobalExceptionHandler convierte en HTTP 400 sin código en los controladores.
    @Override
    public String subirWebp(String carpeta, String nombreBase, MultipartFile archivo)
            throws IOException, InterruptedException {
        if (!esWebp(archivo)) {
            throw new ValidacionException("La imagen debe estar en formato .webp");
        }
        String nombre = slug(nombreBase) + "-" + System.currentTimeMillis() + ".webp";
        return subir(carpeta, nombre, archivo.getBytes(), "image/webp");
    }

    /**
     * Verifica si el archivo recibido no está vacío y corresponde al formato WebP.
     *
     * @param f archivo multipart a inspeccionar
     * @return {@code true} si tiene tipo MIME {@code image/webp} o extensión {@code .webp}
     */
    public static boolean esWebp(MultipartFile f) {
        if (f == null || f.isEmpty()) return false;
        String nombre = f.getOriginalFilename() == null ? "" : f.getOriginalFilename().toLowerCase();
        return "image/webp".equals(f.getContentType()) || nombre.endsWith(".webp");
    }

    @Override
    public String rutaDesdeUrl(String url) {
        if (url == null) return null;
        String prefijo = urlPublica("");
        return url.startsWith(prefijo) ? url.substring(prefijo.length()) : null;
    }

    @Override
    public void eliminarPorUrl(String url) {
        String ruta = rutaDesdeUrl(url);
        if (ruta != null && estaConfigurado()) {
            eliminar(List.of(ruta));
        }
    }

    // REFACTOR: Move Method
    // RAZON: Antes la normalización de nombres a rutas existía duplicada como aCarpeta(...) dentro de AdminProductoController y
    // como slug(...) aquí, y rutaDesdeUrl también estaba copiada en el controlador. Ahora slug, rutaDesdeUrl y
    // urlPublica viven solo en el servicio de Storage y ProductoAdminService los reutiliza (la cadena vacía produce
    // "imagen" en lugar de "producto").
    /**
     * Normaliza un texto eliminando acentos y caracteres especiales para usarlo como nombre de ruta.
     *
     * @param texto texto de entrada
     * @return identificador seguro en minúsculas y separado por guiones
     */
    public static String slug(String texto) {
        String s = Normalizer.normalize(texto == null ? "" : texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "imagen" : s;
    }

    @Override
    public String urlPublica(String ruta) {
        return url + "/storage/v1/object/public/" + bucket + "/" + ruta;
    }

    // REFACTOR: Replace Error Code with Exception
    // RAZON: Antes eliminar ignoraba en silencio cualquier error (catch vacío con el comentario "se ignora a propósito"). Ahora
    // sigue sin propagar el fallo (es una limpieza de mejor esfuerzo que no debe romper la operación principal), pero
    // lo registra con Logger (WARNING) y restaura la bandera de interrupción del hilo. Se prueba con
    // eliminarNoPropagaExcepcionSiFallaRed.
    @Override
    public void eliminar(List<String> rutas) {
        if (rutas == null || rutas.isEmpty()) return;
        try {
            String cuerpo = "{\"prefixes\":[" + rutas.stream()
                    .map(r -> "\"" + r + "\"").collect(Collectors.joining(",")) + "]}";
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/storage/v1/object/" + bucket))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();
            HttpResponse<String> resp = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                LOG.log(Level.WARNING, "Supabase no pudo eliminar {0}: HTTP {1}", new Object[]{rutas, resp.statusCode()});
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Interrupción al limpiar archivos en Supabase", e);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudieron eliminar archivos en Supabase: " + rutas, e);
        }
    }
}
