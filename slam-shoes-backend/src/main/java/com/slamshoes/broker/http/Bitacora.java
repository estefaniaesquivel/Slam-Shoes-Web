package com.slamshoes.broker.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 * 
 * REFACTOR: Move Function (la clase completa pasa de broker.red a broker.http)
 * RAZON: el archivo estaba en el paquete {@code broker.red}, que era el 'paquete de sockets'. Esta clase no
 * depende de ninguna red: se movio a {@code broker.http} junto al resto del transporte actual, sin cambiar su
 * comportamiento, para poder borrar el paquete {@code red} completo.
 */
public final class Bitacora {

    private static final int MAXIMO_EN_MEMORIA = 100;
    // REFACTOR: Replace Magic Literal
    // RAZON: Antes el archivo crecia sin limite (cada solicitud agregaba una linea para siempre). El limite es una
    // constante con nombre: al pasar de 2 MB el archivo se mueve a '<archivo>.1' y se empieza uno nuevo, asi el disco
    // (efimero y pequeno en Render) nunca se llena por la bitacora.
    private static final long MAXIMO_BYTES_ARCHIVO = 2L * 1024 * 1024;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Path archivo;
    private final Deque<String> recientes = new ArrayDeque<>();
    private boolean archivoFallo = false;

    private final long maximoBytes;

    /** @param archivo ruta del archivo de bitácora; null para llevarla solo en memoria */
    public Bitacora(Path archivo) {
        this(archivo, MAXIMO_BYTES_ARCHIVO);
    }

    /**
     * @param archivo     ruta del archivo de bitácora; null para llevarla solo en memoria
     * @param maximoBytes tamaño a partir del cual el archivo se rota a {@code <archivo>.1}
     */
    public Bitacora(Path archivo, long maximoBytes) {
        this.archivo = archivo;
        this.maximoBytes = maximoBytes;
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
            rotarSiHaceFalta();// REFACTOR: Extract Function (uso del metodo)
            Files.writeString(archivo, linea + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Un problema con el archivo no debe tumbar al Broker: se avisa una vez y sigue en memoria
            archivoFallo = true;
            System.out.println("[BROKER]   No se pudo escribir la bitacora en " + archivo + ": " + e.getMessage());
        }
    }

    // REFACTOR: Extract Function
    // RAZON: Antes escribir() solo hacia append; la regla "no dejar crecer el archivo" no existia. Al extraerla en un
    // metodo con nombre, escribir() sigue siendo corto y la rotacion se puede probar por separado (BitacoraTest).
    /** Si el archivo ya pasó del tamaño máximo lo mueve a {@code <archivo>.1}, reemplazando el anterior. */
    private void rotarSiHaceFalta() throws IOException {
        if (Files.exists(archivo) && Files.size(archivo) >= maximoBytes) {
            Files.move(archivo, archivo.resolveSibling(archivo.getFileName() + ".1"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
