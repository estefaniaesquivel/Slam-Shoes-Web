package com.slamshoes.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Sube y borra imágenes en Supabase Storage usando su API REST.
// Las "carpetas" de Storage se crean solas al subir un archivo con ruta "carpeta/archivo".
@Service
public class SupabaseStorageService {

    @Value("${supabase.url:https://ckparlqrfkzofsvskmbw.supabase.co}")
    private String url;

    @Value("${supabase.bucket:slam-shoes-storage}")
    private String bucket;

    @Value("${supabase.service-key:}")
    private String serviceKey;

    private final HttpClient http = HttpClient.newHttpClient();

    public boolean estaConfigurado() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    // Sube un archivo a "carpeta/archivo" y devuelve su URL pública. No sobrescribe archivos existentes.
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

    // Valida que sea un .webp, lo sube a "carpeta/<nombre>-<marca de tiempo>.webp" y devuelve su URL pública
    public String subirWebp(String carpeta, String nombreBase, MultipartFile archivo)
            throws IOException, InterruptedException {
        if (!esWebp(archivo)) throw new IllegalArgumentException("La imagen debe estar en formato .webp");
        String nombre = slug(nombreBase) + "-" + System.currentTimeMillis() + ".webp";
        return subir(carpeta, nombre, archivo.getBytes(), "image/webp");
    }

    public static boolean esWebp(MultipartFile f) {
        if (f == null || f.isEmpty()) return false;
        String nombre = f.getOriginalFilename() == null ? "" : f.getOriginalFilename().toLowerCase();
        return "image/webp".equals(f.getContentType()) || nombre.endsWith(".webp");
    }

    // De una URL pública del bucket obtiene la ruta "carpeta/archivo" (null si no es de Storage)
    public String rutaDesdeUrl(String url) {
        if (url == null) return null;
        String prefijo = urlPublica("");
        return url.startsWith(prefijo) ? url.substring(prefijo.length()) : null;
    }

    // Borra una imagen a partir de su URL pública (mejor esfuerzo)
    public void eliminarPorUrl(String url) {
        String ruta = rutaDesdeUrl(url);
        if (ruta != null && estaConfigurado()) eliminar(List.of(ruta));
    }

    // "Nike Kyrie 8 N7" -> "nike-kyrie-8-n7" (sin acentos ni caracteres que Storage no acepta)
    public static String slug(String texto) {
        String s = Normalizer.normalize(texto == null ? "" : texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "imagen" : s;
    }

    public String urlPublica(String ruta) {
        return url + "/storage/v1/object/public/" + bucket + "/" + ruta;
    }

    // Borra archivos subidos (limpieza si algo falla). Mejor esfuerzo: los errores se ignoran.
    public void eliminar(List<String> rutas) {
        if (rutas.isEmpty()) return;
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
            http.send(peticion, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // se ignora a propósito
        }
    }
}