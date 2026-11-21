package com.slamshoes.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * Contrato para el servicio de almacenamiento de archivos e imágenes del catálogo.
 */
public interface IStorageService {

    /**
     * Verifica si el servicio cuenta con credenciales configuradas para operar.
     *
     * @return {@code true} si la llave de servicio está presente y no vacía
     */
    boolean estaConfigurado();

    /**
     * Sube un archivo binario a una carpeta y devuelve su URL pública.
     *
     * @param carpeta carpeta destino en el bucket
     * @param archivo nombre del archivo destino
     * @param contenido bytes del archivo
     * @param contentType tipo MIME del contenido
     * @return URL pública del recurso almacenado
     * @throws IOException si ocurre un error de red o respuesta HTTP no exitosa
     * @throws InterruptedException si el hilo es interrumpido durante la petición
     */
    String subir(String carpeta, String archivo, byte[] contenido, String contentType)
            throws IOException, InterruptedException;

    /**
     * Valida que el archivo sea WebP, lo sube con un nombre único y devuelve su URL pública.
     *
     * @param carpeta carpeta destino en el bucket
     * @param nombreBase texto base para construir el slug del archivo
     * @param archivo archivo multipart recibido
     * @return URL pública de la imagen subida
     * @throws IOException si ocurre un error de red o respuesta HTTP no exitosa
     * @throws InterruptedException si el hilo es interrumpido durante la petición
     */
    String subirWebp(String carpeta, String nombreBase, MultipartFile archivo)
            throws IOException, InterruptedException;

    /**
     * Extrae la ruta interna del bucket a partir de una URL pública.
     *
     * @param url URL pública completa
     * @return ruta relativa {@code carpeta/archivo} o {@code null} si no pertenece al bucket
     */
    String rutaDesdeUrl(String url);

    /**
     * Elimina un archivo del almacenamiento a partir de su URL pública.
     *
     * @param url URL pública del archivo a eliminar
     */
    void eliminarPorUrl(String url);

    /**
     * Construye la URL pública correspondiente a una ruta interna del bucket.
     *
     * @param ruta ruta relativa dentro del bucket
     * @return URL pública completa
     */
    String urlPublica(String ruta);

    /**
     * Elimina una lista de archivos del bucket por su ruta relativa.
     *
     * @param rutas rutas relativas a eliminar
     */
    void eliminar(List<String> rutas);
}
