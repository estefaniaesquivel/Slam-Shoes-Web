package com.slamshoes.broker.red;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * Convierte cualquier {@link ProveedorServicio} en un PROCESO independiente: escucha por un
 * socket TCP, recibe solicitudes JSON del Broker, ejecuta la operación y contesta JSON.
 *
 * Además se REGISTRA SOLO en el Broker (registro dinámico) y repite el aviso cada cierto tiempo
 * (latido): si el Broker se reinicia o lo retiró por fallos, el proveedor vuelve a aparecer solo.
 * Al detenerse se da de baja.
 *
 * El proveedor no sabe quién es el cliente: solo habla con el Broker.
 */
public class ProveedorServidor {

    private static final int MS_INACTIVIDAD_CONEXION = 30_000;

    private final ProveedorServicio proveedor;
    private final String nombre;
    private final String hostPublico;
    private final int puertoSolicitado;
    private final String brokerHost;
    private final int brokerPuerto;
    private final boolean reemplazar;
    private final long msLatido;

    private final CacheDuplicados<Respuesta> duplicados = new CacheDuplicados<>(1000);
    private final AtomicBoolean activo = new AtomicBoolean(false);
    private final AtomicInteger numeroConexion = new AtomicInteger();
    private volatile ServerSocket servidor;
    private volatile ThreadPoolExecutor pool;
    private Thread aceptador;
    private Thread registrador;

    // Estado del registro, solo para no repetir el mismo mensaje en consola
    private boolean registrado = false;
    private boolean brokerCaidoAvisado = false;
    private boolean rechazoAvisado = false;

    /**
     * @param proveedor        la lógica de negocio a exponer
     * @param hostPublico      nombre con el que el Broker debe contactar a este proveedor (p. ej. "localhost")
     * @param puerto           puerto TCP propio (0 = elegir uno libre)
     * @param brokerHost       dónde está el Broker
     * @param brokerPuerto     puerto del Broker
     * @param reemplazar       si es true y ya hay otro proveedor con ese nombre, lo sustituye (solo al primer registro)
     * @param msLatido         cada cuántos milisegundos avisa al Broker que sigue vivo
     */
    public ProveedorServidor(ProveedorServicio proveedor, String hostPublico, int puerto, String brokerHost,
                             int brokerPuerto, boolean reemplazar, long msLatido) {
        this.proveedor = Objects.requireNonNull(proveedor);
        this.nombre = proveedor.getNombreServicio();
        this.hostPublico = hostPublico;
        this.puertoSolicitado = puerto;
        this.brokerHost = brokerHost;
        this.brokerPuerto = brokerPuerto;
        this.reemplazar = reemplazar;
        this.msLatido = msLatido;
    }

    public ProveedorServidor(ProveedorServicio proveedor, int puerto, String brokerHost, int brokerPuerto,
                             boolean reemplazar) {
        this(proveedor, "localhost", puerto, brokerHost, brokerPuerto, reemplazar, 5000);
    }

    // ------------------------------------------------------------------ ciclo de vida

    public synchronized void iniciar() throws IOException {
        if (activo.get()) {
            return;
        }
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(InetAddress.getByName(Protocolo.direccionEscucha()), puertoSolicitado));
        servidor = socket;

        pool = new ThreadPoolExecutor(8, 64, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
            Thread hilo = new Thread(r, "proveedor-" + nombre + "-conexion-" + numeroConexion.incrementAndGet());
            hilo.setDaemon(true);
            return hilo;
        });

        activo.set(true);
        aceptador = new Thread(this::aceptarConexiones, "proveedor-" + nombre + "-aceptador");
        aceptador.setDaemon(true);
        aceptador.start();
        Consola.proveedor(nombre, "Escuchando en el puerto " + socket.getLocalPort()
                + ", operaciones: " + new TreeSet<>(proveedor.getOperacionesSoportadas()));

        registrador = new Thread(this::mantenerRegistro, "proveedor-" + nombre + "-registro");
        registrador.setDaemon(true);
        registrador.start();
    }

    /** Se da de baja en el Broker (si puede) y cierra el puerto. */
    public synchronized void detener() {
        if (!activo.getAndSet(false)) {
            return;
        }
        if (registrador != null) {
            registrador.interrupt();
        }
        try {
            String respuesta = Canal.intercambiar(brokerHost, brokerPuerto, mensajeBaja(), 1000, 1500);
            Consola.proveedor(nombre, "Baja en el Broker: " + Marshalling.leerMensaje(respuesta).get("mensaje"));
        } catch (IOException | RuntimeException e) {
            Consola.proveedor(nombre, "No se pudo avisar la baja al Broker (" + e.getClass().getSimpleName() + ")");
        }
        try {
            if (servidor != null) {
                servidor.close();
            }
        } catch (IOException e) {
            // ya cerrado
        }
        if (pool != null) {
            pool.shutdownNow();
        }
        Consola.proveedor(nombre, "Detenido");
    }

    /**
     * Simula que el proceso del proveedor se cae de golpe: cierra el puerto SIN darse de baja en el
     * Broker (solo para pruebas; en la vida real nadie avisa cuando un proceso muere).
     */
    public synchronized void detenerSinAvisar() {
        if (!activo.getAndSet(false)) {
            return;
        }
        if (registrador != null) {
            registrador.interrupt();
        }
        try {
            if (servidor != null) {
                servidor.close();
            }
        } catch (IOException e) {
            // ya cerrado
        }
        if (pool != null) {
            pool.shutdownNow();
        }
        Consola.proveedor(nombre, "CAIDA simulada (sin avisar al Broker)");
    }

    public int getPuerto() {
        ServerSocket s = servidor;
        return s == null ? puertoSolicitado : s.getLocalPort();
    }

    public String getNombre() {
        return nombre;
    }

    public boolean estaRegistrado() {
        return registrado;
    }

    // ------------------------------------------------------------------ registro dinámico

    private void mantenerRegistro() {
        boolean primero = true;
        while (activo.get()) {
            registrarEnBroker(primero && reemplazar);
            primero = primero && !registrado;
            try {
                // Mientras no logre registrarse intenta cada segundo; ya registrado, cada msLatido
                Thread.sleep(registrado ? msLatido : Math.min(1000, msLatido));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void registrarEnBroker(boolean pedirReemplazo) {
        try {
            String linea = Canal.intercambiar(brokerHost, brokerPuerto, mensajeRegistro(pedirReemplazo), 1500, 3000);
            Map<String, Object> ack = Marshalling.leerMensaje(linea);
            boolean ok = Boolean.TRUE.equals(ack.get("ok"));
            if (ok && !registrado) {
                Consola.proveedor(nombre, "Registrado en el Broker " + brokerHost + ":" + brokerPuerto
                        + " -> " + ack.get("mensaje"));
            } else if (!ok && !rechazoAvisado) {
                Consola.proveedor(nombre, "El Broker rechazo el registro: " + ack.get("mensaje"));
                rechazoAvisado = true;
            }
            registrado = ok;
            if (ok) {
                rechazoAvisado = false;
            }
            brokerCaidoAvisado = false;
        } catch (IOException | RuntimeException e) {
            if (registrado || !brokerCaidoAvisado) {
                Consola.proveedor(nombre, "No se pudo contactar al Broker (" + brokerHost + ":" + brokerPuerto
                        + "); se reintentara automaticamente");
            }
            registrado = false;
            brokerCaidoAvisado = true;
        }
    }

    private String mensajeRegistro(boolean pedirReemplazo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(Protocolo.TIPO, Protocolo.REGISTRAR);
        m.put("servicio", nombre);
        m.put("host", hostPublico);
        m.put("puerto", getPuerto());
        m.put("operaciones", new TreeSet<>(proveedor.getOperacionesSoportadas()));
        m.put("reemplazar", pedirReemplazo);
        return Json.escribir(m);
    }

    private String mensajeBaja() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(Protocolo.TIPO, Protocolo.BAJA);
        m.put("servicio", nombre);
        m.put("host", hostPublico);
        m.put("puerto", getPuerto());
        return Json.escribir(m);
    }

    // ------------------------------------------------------------------ atención de solicitudes

    private void aceptarConexiones() {
        while (activo.get()) {
            Socket socket;
            try {
                socket = servidor.accept();
            } catch (SocketException e) {
                return; // el servidor se cerró
            } catch (IOException e) {
                continue;
            }
            if (!activo.get()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // nada que hacer
                }
                return;
            }
            try {
                pool.execute(() -> atender(socket));
            } catch (RuntimeException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    // nada que hacer
                }
            }
        }
    }

    private void atender(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(MS_INACTIVIDAD_CONEXION);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            Writer salida = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            String linea;
            while ((linea = Canal.leerLinea(entrada)) != null) {
                Canal.escribirLinea(salida, procesar(linea));
            }
        } catch (SocketTimeoutException e) {
            // conexión inactiva
        } catch (IOException e) {
            // el Broker cortó la conexión (por ejemplo, agotó su timeout): no es un error del proveedor
        }
    }

    /** Recibe un mensaje JSON y devuelve SIEMPRE una Respuesta JSON. */
    private String procesar(String linea) {
        Solicitud solicitud;
        try {
            solicitud = Marshalling.unmarshalSolicitud(Marshalling.leerMensaje(linea));        // unmarshalling
        } catch (IllegalArgumentException e) {
            Consola.proveedor(nombre, "Mensaje invalido: " + e.getMessage());
            return Marshalling.marshalRespuesta(Respuesta.error("sin-id", EstadoRespuesta.SOLICITUD_INVALIDA,
                    "Solicitud mal formada: " + e.getMessage()));
        }

        Consola.proveedor(nombre, "Solicitud " + solicitud.getIdCorto() + " " + solicitud.getOperacion()
                + " " + solicitud.getParametrosParaLog());
        CacheDuplicados.Resultado<Respuesta> resultado = duplicados.ejecutar(
                solicitud.getIdSolicitud(), () -> ejecutarSeguro(solicitud), Respuesta::isExitosa);
        if (resultado.duplicada) {
            Consola.proveedor(nombre, "Solicitud " + solicitud.getIdCorto()
                    + " DUPLICADA: no se vuelve a ejecutar, se devuelve la respuesta anterior");
        }
        return Marshalling.marshalRespuesta(resultado.valor);                                  // marshalling
    }

    /** Ejecuta la operación; cualquier excepción se convierte en una respuesta ERROR_PROVEEDOR. */
    private Respuesta ejecutarSeguro(Solicitud solicitud) {
        String id = solicitud.getIdSolicitud();
        try {
            Respuesta respuesta = proveedor.ejecutar(solicitud);
            if (respuesta == null) {
                return Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR, "El proveedor devolvio una respuesta nula");
            }
            if (!id.equals(respuesta.getIdSolicitud())) {
                return Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR,
                        "El proveedor devolvio una respuesta con otro idSolicitud");
            }
            return respuesta;
        } catch (Exception e) {
            Consola.proveedor(nombre, "EXCEPCION en " + solicitud.getOperacion() + ": " + e);
            return Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR,
                    "El proveedor fallo: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
