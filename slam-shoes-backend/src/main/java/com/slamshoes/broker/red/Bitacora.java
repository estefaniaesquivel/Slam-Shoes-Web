package com.slamshoes.broker.red;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.slamshoes.broker.EstadoRespuesta;

/**
 * Bitácora de solicitudes del Broker: una línea por solicitud atendida, en un archivo de texto
 * (si se indicó) y en memoria (las últimas 100, para el monitoreo).
 *
 * Formato:  fecha | id | servicio.operacion | estado | duración | detalle
 */
public final class Bitacora {

    private static final int MAXIMO_EN_MEMORIA = 100;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Path archivo;
    private final Deque<String> recientes = new ArrayDeque<>();
    private boolean archivoFallo = false;

    /** @param archivo ruta del archivo de bitácora; null para llevarla solo en memoria */
    public Bitacora(Path archivo) {
        this.archivo = archivo;
    }

    public void registrar(String idCorto, String servicio, String operacion, EstadoRespuesta estado,
                          long milisegundos, String detalle) {
        escribir(String.join(" | ", FORMATO.format(LocalDateTime.now()), idCorto,
                servicio + "." + operacion, String.valueOf(estado), milisegundos + " ms",
                detalle == null ? "-" : detalle));
    }

    /** Evento que no es una solicitud (registro de un proveedor, baja, etc.). */
    public void evento(String texto) {
        escribir(String.join(" | ", FORMATO.format(LocalDateTime.now()), "--------", "EVENTO", texto));
    }

    /** Copia de las últimas líneas (la más reciente al final). */
    public synchronized List<String> ultimas() {
        return new ArrayList<>(recientes);
    }

    private synchronized void escribir(String linea) {
        recientes.addLast(linea);
        while (recientes.size() > MAXIMO_EN_MEMORIA) {
            recientes.removeFirst();
        }
        if (archivo == null || archivoFallo) {
            return;
        }
        try {
            Files.writeString(archivo, linea + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Un problema con el archivo no debe tumbar al Broker: se avisa una vez y sigue en memoria
            archivoFallo = true;
            System.out.println("[BROKER]   No se pudo escribir la bitacora en " + archivo + ": " + e.getMessage());
        }
    }
}
