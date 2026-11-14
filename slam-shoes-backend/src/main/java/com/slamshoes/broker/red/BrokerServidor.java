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
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.slamshoes.broker.Broker;
import com.slamshoes.broker.BrokerCentral;
import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.RegistroServicios;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * Broker del Nivel 2: el mismo {@link BrokerCentral} del Nivel 1, pero escuchando por un socket TCP.
 *
 * <ul>
 *   <li><b>Sockets + hilos</b>: un hilo (de un pool acotado) atiende cada conexión, así que varios
 *       clientes y proveedores se atienden a la vez.</li>
 *   <li><b>JSON</b>: recibe/entrega líneas JSON ({@link Marshalling}).</li>
 *   <li><b>Registro dinámico</b>: los proveedores se registran/dan de baja ellos mismos mientras el
 *       Broker está corriendo (mensajes REGISTRAR / BAJA).</li>
 *   <li><b>Timeouts y reintentos</b>: se aplican en {@link ProveedorRemoto}; las conexiones
 *       inactivas se cierran.</li>
 *   <li><b>Bitácora</b>: cada solicitud queda en archivo y en memoria ({@link Bitacora}).</li>
 *   <li><b>Duplicados</b>: una solicitud repetida (mismo id) devuelve la respuesta anterior.</li>
 * </ul>
 */
public class BrokerServidor {

    private static final int MS_INACTIVIDAD_CONEXION = 30_000;
    private static final int MAXIMO_HILOS = 128;
    /** Fallos consecutivos tras los cuales un proveedor remoto se retira del registro. */
    private static final int MAXIMO_FALLOS_CONSECUTIVOS = 3;

    private final int puertoSolicitado;
    private final RegistroServicios registro = new RegistroServicios();
    private final Broker broker = new BrokerCentral(registro);
    private final Bitacora bitacora;
    private final CacheDuplicados<Respuesta> duplicados = new CacheDuplicados<>(1000);

    // Configuración de la comunicación Broker -> Proveedor
    private final int msConexionProveedor;
    private final int msLecturaProveedor;
    private final int reintentosProveedor;

    // Métricas para el monitoreo
    private final LocalDateTime iniciadoEn = LocalDateTime.now();
    private final AtomicLong solicitudesTotales = new AtomicLong();
    private final AtomicLong solicitudesDuplicadas = new AtomicLong();
    private final AtomicLong milisegundosAcumulados = new AtomicLong();
    private final Map<EstadoRespuesta, AtomicLong> porEstado = new ConcurrentHashMap<>();
    private final Set<String> rechazosAvisados = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean activo = new AtomicBoolean(false);
    private final AtomicInteger numeroConexion = new AtomicInteger();
    private volatile ServerSocket servidor;
    private volatile ThreadPoolExecutor pool;
    private Thread aceptador;

    public BrokerServidor(int puerto, Path archivoBitacora) {
        this(puerto, archivoBitacora, 2000, 5000, 2);
    }

    /**
     * @param puerto              puerto TCP (0 = que el sistema elija uno libre)
     * @param archivoBitacora     archivo de la bitácora (null = solo en memoria)
     * @param msConexionProveedor tiempo máximo para conectarse a un proveedor
     * @param msLecturaProveedor  tiempo máximo esperando la respuesta de un proveedor
     * @param reintentosProveedor reintentos limitados tras un fallo de red con un proveedor
     */
    public BrokerServidor(int puerto, Path archivoBitacora, int msConexionProveedor, int msLecturaProveedor,
                          int reintentosProveedor) {
        this.puertoSolicitado = puerto;
        this.bitacora = new Bitacora(archivoBitacora);
        this.msConexionProveedor = msConexionProveedor;
        this.msLecturaProveedor = msLecturaProveedor;
        this.reintentosProveedor = reintentosProveedor;
    }

    // ------------------------------------------------------------------ ciclo de vida

    /** Abre el puerto y empieza a aceptar conexiones. @throws IOException si el puerto está ocupado */
    public synchronized void iniciar() throws IOException {
        if (activo.get()) {
            return;
        }
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(InetAddress.getByName(Protocolo.direccionEscucha()), puertoSolicitado));
        servidor = socket;

        pool = new ThreadPoolExecutor(16, MAXIMO_HILOS, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
            Thread hilo = new Thread(r, "broker-conexion-" + numeroConexion.incrementAndGet());
            hilo.setDaemon(true);
            return hilo;
        });

        activo.set(true);
        aceptador = new Thread(this::aceptarConexiones, "broker-aceptador");
        aceptador.setDaemon(true);
        aceptador.start();
        Consola.broker("Broker escuchando en " + socket.getInetAddress().getHostAddress() + ":" + socket.getLocalPort()
                + " (timeout proveedor " + msLecturaProveedor + " ms, reintentos " + reintentosProveedor + ")");
        bitacora.evento("Broker iniciado en el puerto " + socket.getLocalPort());
    }

    /** Cierra el puerto y las conexiones en curso. */
    public synchronized void detener() {
        if (!activo.getAndSet(false)) {
            return;
        }
        try {
            if (servidor != null) {
                servidor.close();
            }
        } catch (IOException e) {
            // ya está cerrado
        }
        if (pool != null) {
            pool.shutdownNow();
        }
        // Espera a que el hilo aceptador termine, para que el puerto quede realmente liberado
        if (aceptador != null && aceptador != Thread.currentThread()) {
            try {
                aceptador.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Consola.broker("Broker detenido");
        bitacora.evento("Broker detenido");
    }

    /** Puerto realmente abierto (útil cuando se pidió el puerto 0). */
    public int getPuerto() {
        ServerSocket s = servidor;
        return s == null ? puertoSolicitado : s.getLocalPort();
    }

    public RegistroServicios getRegistro() {
        return registro;
    }

    public Bitacora getBitacora() {
        return bitacora;
    }

    public boolean estaActivo() {
        return activo.get();
    }

    // ------------------------------------------------------------------ aceptación de conexiones

    private void aceptarConexiones() {
        while (activo.get()) {
            Socket socket;
            try {
                socket = servidor.accept();
            } catch (SocketException e) {
                if (activo.get()) {
                    Consola.broker("Error aceptando conexiones: " + e.getMessage());
                }
                return;
            } catch (IOException e) {
                Consola.broker("Error aceptando conexiones: " + e.getMessage());
                continue;
            }
            if (!activo.get()) {
                // Llego una conexion justo mientras el Broker se apagaba: se cierra sin atenderla
                cerrarSilencioso(socket);
                return;
            }
            try {
                pool.execute(() -> atender(socket));
            } catch (RejectedExecutionException e) {
                if (!activo.get()) {
                    cerrarSilencioso(socket);
                    return;
                }
                Consola.broker("Broker saturado: conexion rechazada");
                responderSaturado(socket);
            }
        }
    }

    private static void cerrarSilencioso(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // nada que hacer
        }
    }

    private void responderSaturado(Socket socket) {
        try (Socket s = socket) {
            Writer salida = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            Canal.escribirLinea(salida, Marshalling.marshalRespuesta(
                    Respuesta.error("sin-id", EstadoRespuesta.ERROR_PROVEEDOR, "El Broker esta saturado, intenta mas tarde")));
        } catch (IOException e) {
            // el cliente ya se fue
        }
    }

    /** Atiende una conexión: lee líneas JSON y contesta una línea por cada una, hasta que el otro lado cierre. */
    private void atender(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(MS_INACTIVIDAD_CONEXION);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            Writer salida = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            String linea;
            while ((linea = Canal.leerLinea(entrada)) != null) {
                String respuesta = procesarLinea(linea, s.getInetAddress().getHostAddress());
                Canal.escribirLinea(salida, respuesta);
            }
        } catch (SocketTimeoutException e) {
            Consola.broker("Conexion inactiva cerrada");
        } catch (IOException e) {
            if (activo.get()) {
                Consola.broker("Conexion cortada: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ procesamiento de mensajes

    /** Devuelve SIEMPRE una línea JSON, aunque el mensaje recibido esté mal formado. */
    String procesarLinea(String linea, String origen) {
        try {
            Map<String, Object> mensaje;
            try {
                mensaje = Marshalling.leerMensaje(linea);                                  // unmarshalling
            } catch (IllegalArgumentException e) {
                return rechazarSolicitud("sin-id", "Solicitud mal formada: " + e.getMessage(), origen);
            }
            Object tipo = mensaje.get(Protocolo.TIPO);
            String nombreTipo = tipo == null ? Protocolo.SOLICITUD : tipo.toString();
            switch (nombreTipo) {
                case Protocolo.SOLICITUD:
                    return procesarSolicitud(mensaje, origen);
                case Protocolo.REGISTRAR:
                    return registrarProveedor(mensaje);
                case Protocolo.BAJA:
                    return darDeBaja(mensaje);
                case Protocolo.ESTADO:
                    return Json.escribir(estado());
                default:
                    return rechazarSolicitud("sin-id", "Tipo de mensaje desconocido: '" + nombreTipo + "'", origen);
            }
        } catch (RuntimeException e) {
            Consola.broker("Error interno al procesar un mensaje: " + e);
            return Marshalling.marshalRespuesta(Respuesta.error("sin-id", EstadoRespuesta.ERROR_PROVEEDOR,
                    "Error interno del Broker: " + e.getClass().getSimpleName()));
        }
    }

    private String rechazarSolicitud(String id, String mensaje, String origen) {
        Consola.broker("Solicitud rechazada (" + origen + "): " + mensaje);
        contar(EstadoRespuesta.SOLICITUD_INVALIDA, 0);
        bitacora.registrar(Consola.corto(id), "?", "?", EstadoRespuesta.SOLICITUD_INVALIDA, 0, mensaje);
        return Marshalling.marshalRespuesta(Respuesta.error(id, EstadoRespuesta.SOLICITUD_INVALIDA, mensaje));
    }

    private String procesarSolicitud(Map<String, Object> mensaje, String origen) {
        Solicitud solicitud;
        try {
            solicitud = Marshalling.unmarshalSolicitud(mensaje);
        } catch (IllegalArgumentException e) {
            Object id = mensaje.get("idSolicitud");
            return rechazarSolicitud(id instanceof String texto && !texto.isBlank() ? texto : "sin-id",
                    "Solicitud mal formada: " + e.getMessage(), origen);
        }

        long inicio = System.nanoTime();
        CacheDuplicados.Resultado<Respuesta> resultado = duplicados.ejecutar(
                solicitud.getIdSolicitud(),
                () -> broker.procesar(solicitud),            // BrokerCentral: busca el proveedor y reenvía
                Respuesta::isExitosa);                         // solo se recuerdan las exitosas
        Respuesta respuesta = resultado.valor;
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        if (resultado.duplicada) {
            solicitudesDuplicadas.incrementAndGet();
            Consola.broker("Solicitud " + solicitud.getIdCorto() + " DUPLICADA: se devuelve la respuesta ya calculada");
        } else {
            vigilarProveedor(solicitud.getServicio(), respuesta);
        }
        contar(respuesta.getEstado(), ms);
        bitacora.registrar(solicitud.getIdCorto(), solicitud.getServicio(), solicitud.getOperacion(),
                respuesta.getEstado(), ms,
                (resultado.duplicada ? "[duplicada] " : "") + "origen=" + origen
                        + (respuesta.getMensajeError() != null ? " error=" + respuesta.getMensajeError() : ""));
        return Marshalling.marshalRespuesta(respuesta);
    }

    private void contar(EstadoRespuesta estado, long ms) {
        solicitudesTotales.incrementAndGet();
        milisegundosAcumulados.addAndGet(ms);
        porEstado.computeIfAbsent(estado, k -> new AtomicLong()).incrementAndGet();
    }

    /** Si un proveedor remoto falla varias veces seguidas se retira del registro (puede volver a registrarse). */
    private void vigilarProveedor(String servicio, Respuesta respuesta) {
        EstadoRespuesta estado = respuesta.getEstado();
        if (estado != EstadoRespuesta.PROVEEDOR_NO_DISPONIBLE && estado != EstadoRespuesta.TIEMPO_AGOTADO) {
            return;
        }
        registro.buscar(servicio).ifPresent(p -> {
            if (p instanceof ProveedorRemoto remoto && remoto.getFallosConsecutivos() >= MAXIMO_FALLOS_CONSECUTIVOS
                    && registro.eliminar(servicio, remoto)) {
                Consola.broker("Proveedor '" + servicio + "' (" + remoto.destino() + ") retirado tras "
                        + remoto.getFallosConsecutivos() + " fallos consecutivos; volvera cuando se registre de nuevo");
                bitacora.evento("Proveedor retirado por fallos: " + servicio + " @ " + remoto.destino());
            }
        });
    }

    // ------------------------------------------------------------------ registro dinámico

    private String registrarProveedor(Map<String, Object> mensaje) {
        String servicio = texto(mensaje.get("servicio"));
        String host = texto(mensaje.get("host"));
        Object puertoCrudo = mensaje.get("puerto");
        boolean reemplazar = Boolean.TRUE.equals(mensaje.get("reemplazar"));

        Set<String> operaciones = new HashSet<>();
        if (mensaje.get("operaciones") instanceof List<?> lista) {
            for (Object o : lista) {
                if (o != null && !o.toString().isBlank()) {
                    operaciones.add(o.toString().trim());
                }
            }
        }
        if (servicio == null || host == null || !(puertoCrudo instanceof Number n)
                || n.intValue() < 1 || n.intValue() > 65535 || operaciones.isEmpty()) {
            Consola.registro("REGISTRO RECHAZADO: mensaje incompleto (servicio, host, puerto 1-65535 y operaciones son obligatorios)");
            return ack(false, "Registro rechazado: faltan servicio, host, puerto valido u operaciones");
        }
        int puerto = n.intValue();

        synchronized (registro) {
            ProveedorRemoto nuevo = new ProveedorRemoto(servicio, host, puerto, operaciones,
                    msConexionProveedor, msLecturaProveedor, reintentosProveedor);
            Optional<ProveedorServicio> actual = registro.buscar(servicio);

            if (actual.isEmpty()) {
                registro.registrar(servicio, nuevo);
                bitacora.evento("Proveedor registrado: " + servicio + " @ " + nuevo.destino() + " " + new java.util.TreeSet<>(operaciones));
                return ack(true, "Proveedor '" + servicio + "' registrado");
            }
            if (actual.get() instanceof ProveedorRemoto existente && existente.mismoDestino(host, puerto)) {
                // El mismo proveedor avisando de nuevo (latido o reinicio): no se hace ruido en consola
                if (existente.mismasOperaciones(operaciones)) {
                    existente.tocar();
                } else {
                    registro.sustituir(servicio, nuevo);
                    bitacora.evento("Proveedor actualizado: " + servicio + " @ " + nuevo.destino());
                }
                return ack(true, "Proveedor '" + servicio + "' ya estaba registrado");
            }
            if (reemplazar) {
                registro.sustituir(servicio, nuevo);
                bitacora.evento("Proveedor SUSTITUIDO: " + servicio + " ahora @ " + nuevo.destino());
                return ack(true, "Proveedor '" + servicio + "' sustituido");
            }
            String clave = servicio + "@" + host + ":" + puerto;
            if (rechazosAvisados.add(clave)) {
                Consola.registro("REGISTRO RECHAZADO: '" + servicio + "' ya tiene otro proveedor (" + actual.get()
                        + "); " + host + ":" + puerto + " debe pedir reemplazar=true");
            }
            return ack(false, "Ya existe un proveedor registrado para '" + servicio + "'; usa reemplazar para sustituirlo");
        }
    }

    private String darDeBaja(Map<String, Object> mensaje) {
        String servicio = texto(mensaje.get("servicio"));
        String host = texto(mensaje.get("host"));
        Object puerto = mensaje.get("puerto");
        if (servicio == null || host == null || !(puerto instanceof Number n)) {
            return ack(false, "Baja rechazada: faltan servicio, host o puerto");
        }
        synchronized (registro) {
            Optional<ProveedorServicio> actual = registro.buscar(servicio);
            if (actual.isPresent() && actual.get() instanceof ProveedorRemoto remoto
                    && remoto.mismoDestino(host, n.intValue()) && registro.eliminar(servicio, remoto)) {
                bitacora.evento("Proveedor dado de baja: " + servicio + " @ " + remoto.destino());
                return ack(true, "Proveedor '" + servicio + "' dado de baja");
            }
        }
        return ack(false, "No habia un proveedor '" + servicio + "' registrado desde ese destino");
    }

    private static String texto(Object valor) {
        return valor instanceof String s && !s.isBlank() ? s.trim() : null;
    }

    private static String ack(boolean ok, String mensaje) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(Protocolo.TIPO, Protocolo.ACK);
        m.put("ok", ok);
        m.put("mensaje", mensaje);
        return Json.escribir(m);
    }

    // ------------------------------------------------------------------ monitoreo

    /** Foto del estado del Broker: servicios registrados, métricas y últimas solicitudes. */
    Map<String, Object> estado() {
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put(Protocolo.TIPO, Protocolo.ESTADO);
        raiz.put("puerto", getPuerto());
        raiz.put("iniciadoEn", iniciadoEn);

        List<Map<String, Object>> servicios = new ArrayList<>();
        for (String nombre : registro.listarNombres()) {
            registro.buscar(nombre).ifPresent(p -> {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("servicio", nombre);
                s.put("operaciones", new java.util.TreeSet<>(p.getOperacionesSoportadas()));
                if (p instanceof ProveedorRemoto remoto) {
                    s.put("tipo", "remoto");
                    s.put("host", remoto.getHost());
                    s.put("puerto", remoto.getPuerto());
                    s.put("fallosConsecutivos", remoto.getFallosConsecutivos());
                    s.put("ultimoContacto", remoto.getUltimoContacto());
                } else {
                    s.put("tipo", "local");
                    s.put("clase", p.getClass().getSimpleName());
                }
                servicios.add(s);
            });
        }
        raiz.put("servicios", servicios);

        long total = solicitudesTotales.get();
        Map<String, Object> estados = new LinkedHashMap<>();
        porEstado.forEach((k, v) -> estados.put(k.name(), v.get()));
        long exitosas = porEstado.getOrDefault(EstadoRespuesta.EXITO, new AtomicLong()).get();

        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("solicitudesTotales", total);
        metricas.put("exitosas", exitosas);
        metricas.put("conError", total - exitosas);
        metricas.put("duplicadasAtendidasDesdeCache", solicitudesDuplicadas.get());
        metricas.put("tiempoPromedioMs", total == 0 ? 0 : milisegundosAcumulados.get() / total);
        metricas.put("porEstado", estados);
        raiz.put("metricas", metricas);

        raiz.put("ultimasSolicitudes", bitacora.ultimas());
        return raiz;
    }
}
