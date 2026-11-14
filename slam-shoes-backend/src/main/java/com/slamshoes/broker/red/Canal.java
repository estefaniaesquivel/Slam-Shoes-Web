package com.slamshoes.broker.red;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Comunicación por sockets TCP: enviar UNA línea JSON y leer UNA línea JSON de respuesta,
 * con tiempo límite de conexión y de lectura (timeouts).
 */
public final class Canal {

    /** Tamaño máximo de un mensaje (1 millón de caracteres) para que nadie agote la memoria del servidor. */
    public static final int MAXIMO_LINEA = 1_000_000;

    private Canal() {
    }

    /**
     * Abre una conexión, envía la línea, espera la respuesta y cierra.
     *
     * @throws java.net.SocketTimeoutException si no se pudo conectar o no respondió a tiempo
     * @throws IOException                     si la conexión se rechazó o se cortó
     */
    public static String intercambiar(String host, int puerto, String linea, int msConexion, int msLectura)
            throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), msConexion);
            socket.setSoTimeout(msLectura);
            socket.setTcpNoDelay(true);

            Writer salida = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            salida.write(linea);
            salida.write('\n');
            salida.flush();

            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String respuesta = leerLinea(entrada);
            if (respuesta == null) {
                throw new EOFException("La conexion se cerro antes de recibir la respuesta");
            }
            return respuesta;
        }
    }

    /** Lee hasta el salto de línea; devuelve null si la conexión terminó sin enviar nada. */
    public static String leerLinea(BufferedReader entrada) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = entrada.read()) != -1) {
            if (c == '\n') {
                return sb.toString();
            }
            if (c == '\r') {
                continue;
            }
            if (sb.length() >= MAXIMO_LINEA) {
                throw new IOException("Mensaje demasiado largo (maximo " + MAXIMO_LINEA + " caracteres)");
            }
            sb.append((char) c);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Escribe una línea y vacía el buffer. */
    public static void escribirLinea(Writer salida, String linea) throws IOException {
        salida.write(linea);
        salida.write('\n');
        salida.flush();
    }
}
