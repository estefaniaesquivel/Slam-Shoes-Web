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

// Sube y borra imágenes en Supabase Storage usando su API REST.
// Las "carpetas" de Storage se crean solas al subir un archivo con ruta "carpeta/archivo".
@Service
public class SupabaseStorageService implements IStorageService {

    private static final Logger LOG = Logger.getLogger(SupabaseStorageService.class.getName());

    private final String url;
    private final String bucket;
    private final String serviceKey;
    private final HttpClient http;

    @Autowired
    public SupabaseStorageService(
            @Value("${supabase.url:https://ckparlqrfkzofsvskmbw.supabase.co}") String url,
            @Value("${supabase.bucket:slam-shoes-storage}") String bucket,
            @Value("${supabase.service-key:}") String serviceKey) {
        this(url, bucket, serviceKey, HttpClient.newHttpClient());
    }

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

    @Override
    public String subirWebp(String carpeta, String nombreBase, MultipartFile archivo)
            throws IOException, InterruptedException {
        if (!esWebp(archivo)) {
            throw new ValidacionException("La imagen debe estar en formato .webp");
        }
        String nombre = slug(nombreBase) + "-" + System.currentTimeMillis() + ".webp";
        return subir(carpeta, nombre, archivo.getBytes(), "image/webp");
    }

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

    // "Nike Kyrie 8 N7" -> "nike-kyrie-8-n7" (sin acentos ni caracteres que Storage no acepta)
    public static String slug(String texto) {
        String s = Normalizer.normalize(texto == null ? "" : texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "imagen" : s;
    }

    @Override
    public String urlPublica(String ruta) {
        return url + "/storage/v1/object/public/" + bucket + "/" + ruta;
    }

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
