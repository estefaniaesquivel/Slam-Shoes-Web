package com.slamshoes.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {
    boolean estaConfigurado();
    String subir(String carpeta, String archivo, byte[] contenido, String contentType)
            throws IOException, InterruptedException;
    String subirWebp(String carpeta, String nombreBase, MultipartFile archivo)
            throws IOException, InterruptedException;
    String rutaDesdeUrl(String url);
    void eliminarPorUrl(String url);
    String urlPublica(String ruta);
    void eliminar(List<String> rutas);
}
