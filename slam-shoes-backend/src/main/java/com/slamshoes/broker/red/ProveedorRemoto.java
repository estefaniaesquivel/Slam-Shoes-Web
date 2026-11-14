package com.slamshoes.broker.red;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.ProveedorNoDisponibleException;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * Representante (proxy) de un proveedor que vive en OTRO proceso.
 *
 * Implementa la misma interfaz {@link ProveedorServicio} que los proveedores locales, así que
 * {@link com.slamshoes.broker.BrokerCentral} y {@link com.slamshoes.broker.RegistroServicios}
 * funcionan igual que en el Nivel 1: no saben que "ejecutar" ahora cruza un socket.
 *
 * Hace el marshalling de la solicitud, la envía con timeouts, y si hay fallos de red reintenta un
 * número LIMITADO de veces (con el mismo idSolicitud: el proveedor descarta los duplicados).
 */
public class ProveedorRemoto implements ProveedorServicio {

    private final String nombreServicio;
    private final String host;
    private final int puerto;
    private final Set<String> operaciones;
    private final int msConexion;
    private final int msLectura;
    private final int reintentos;

    private final AtomicInteger fallosConsecutivos = new AtomicInteger();
    private volatile LocalDateTime ultimoContacto = LocalDateTime.now();

    public ProveedorRemoto(String nombreServicio, String host, int puerto, Set<String> operaciones,
                           int msConexion, int msLectura, int reintentos) {
        this.nombreServicio = Objects.requireNonNull(nombreServicio);
        this.host = Objects.requireNonNull(host);
        this.puerto = puerto;
        this.operaciones = new TreeSet<>(operaciones);
        this.msConexion = msConexion;
        this.msLectura = msLectura;
        this.reintentos = Math.max(0, reintentos);
    }

    @Override
    public Respuesta ejecutar(Solicitud solicitud) {
        String json = Marshalling.marshalSolicitud(solicitud);            // marshalling
        int intentos = reintentos + 1;
        IOException ultimoError = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                String linea = Canal.intercambiar(host, puerto, json, msConexion, msLectura);
                Respuesta respuesta;
                try {
                    respuesta = Marshalling.unmarshalRespuesta(Marshalling.leerMensaje(linea)); // unmarshalling
                } catch (IllegalArgumentException e) {
                    // El proveedor contestó algo ilegible: no tiene caso reintentar
                    throw new IllegalStateException("Respuesta ilegible de " + destino() + ": " + e.getMessage());
                }
                fallosConsecutivos.set(0);
                ultimoContacto = LocalDateTime.now();
                return respuesta;
            } catch (IOException e) {
                ultimoError = e;
                Consola.broker("Intento " + intento + "/" + intentos + " con " + destino() + " fallo: " + describir(e));
                if (intento < intentos) {
                    try {
                        Thread.sleep(200L * intento); // espera creciente entre reintentos
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        fallosConsecutivos.incrementAndGet();
        boolean tiempo = ultimoError instanceof SocketTimeoutException;
        throw new ProveedorNoDisponibleException(
                (tiempo ? "El proveedor '" + nombreServicio + "' no respondio a tiempo en "
                        : "No se pudo contactar al proveedor '" + nombreServicio + "' en ")
                        + destino() + " (" + intentos + " intento(s))",
                tiempo, ultimoError);
    }

    @Override
    public String getNombreServicio() {
        return nombreServicio;
    }

    @Override
    public Set<String> getOperacionesSoportadas() {
        return operaciones;
    }

    public String getHost() {
        return host;
    }

    public int getPuerto() {
        return puerto;
    }

    public int getFallosConsecutivos() {
        return fallosConsecutivos.get();
    }

    public LocalDateTime getUltimoContacto() {
        return ultimoContacto;
    }

    /** Marca que el proveedor volvió a avisar que está vivo (nuevo registro / latido). */
    public void tocar() {
        ultimoContacto = LocalDateTime.now();
        fallosConsecutivos.set(0);
    }

    public boolean mismoDestino(String otroHost, int otroPuerto) {
        return host.equalsIgnoreCase(otroHost) && puerto == otroPuerto;
    }

    public boolean mismasOperaciones(Set<String> otras) {
        return operaciones.equals(new TreeSet<>(otras));
    }

    public String destino() {
        return host + ":" + puerto;
    }

    @Override
    public String toString() {
        return "ProveedorRemoto[" + nombreServicio + " @ " + destino() + "]";
    }

    private static String describir(IOException e) {
        return (e instanceof SocketTimeoutException ? "tiempo agotado" : e.getClass().getSimpleName()
                + (e.getMessage() == null ? "" : ": " + e.getMessage()));
    }
}
