package com.slamshoes.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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