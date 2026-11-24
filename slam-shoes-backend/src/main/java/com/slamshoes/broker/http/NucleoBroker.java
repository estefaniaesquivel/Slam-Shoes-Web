package com.slamshoes.broker.http;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
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
 * Corazón del Broker distribuido SIN conocer ni sockets ni Spring: registro dinámico de proveedores,
 * enrutamiento de solicitudes ({@link BrokerCentral}), bitácora, detección de duplicados y métricas.
 *
 * Los controladores REST ({@code BrokerHttpController}) son una capa delgada que solo traduce HTTP a
 * llamadas de esta clase. Por eso se puede probar con JUnit sin levantar la aplicación.
 *
 * Funciones que conserva del Broker de sockets: registro dinámico (ahora {@code POST /api/broker/register}),
 * timeouts y reintentos (en {@link ProveedorHttp}), bitácora, respuesta anterior para solicitudes repetidas
 * y retiro de proveedores que fallan varias veces seguidas (vuelven a aparecer al re-registrarse).
 *
 * REFACTOR: Extract Class
 * RAZÓN: {@code BrokerServidor} mezclaba en 480 líneas dos responsabilidades: ESCUCHAR (ServerSocket, hilos,
 * pool, conexiones) y DECIDIR (registrar, enrutar, contar). Al migrar a HTTP la parte de "escuchar" la hace
 * Tomcat/Spring, así que la lógica de decisión se extrajo a esta clase, que quedó sin dependencias de red y
 * con una sola razón para cambiar.
 *
 * REFACTOR: Split Phase (registro)
 * RAZÓN: antes registrar un proveedor mezclaba leer el mapa crudo con decidir qué hacer. Ahora la fase 1
 * (parsear y validar) es {@link RegistroProveedor#desdeMapa(Map)} y la fase 2 (decidir: alta, latido,
 * actualización, sustitución o rechazo) es {@link #registrar(String, Map, String)}.
 */
public class NucleoBroker {

    /** Resultado de registrar/dar de baja: código HTTP sugerido, si fue aceptado y un mensaje legible. */
    // REFACTOR: Replace Primitive with Object
    // RAZON: {@code BrokerServidor.ack(boolean, String)} construia a mano un String JSON con el campo tipo=ACK.
    // Ahora el resultado de registrar o dar de baja es un objeto con codigo HTTP, ok y mensaje; el controlador lo
    // traduce a 200/400/401/404/409 sin parsear texto.
    public record Acuse(int codigo, boolean ok, String mensaje) {
        /** Mapa JSON-friendly para el cuerpo de la respuesta HTTP. */
        public Map<String, Object> aMapa() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ok", ok);
            m.put("mensaje", mensaje);
            return m;
        }
    }

    private final RegistroServicios registro = new RegistroServicios();
    private final Broker broker = new BrokerCentral(registro);
    private final Bitacora bitacora;
    private final CacheDuplicados<Respuesta> duplicados = new CacheDuplicados<>(1000);
    private final ProxyHttp proxy;

    private final int msConexionProveedor;
    private final int msLecturaProveedor;
    private final int reintentosProveedor;
    private final int maximoFallos;
    private final String token;

    private final LocalDateTime iniciadoEn = LocalDateTime.now();
    private final AtomicLong solicitudesTotales = new AtomicLong();
    private final AtomicLong solicitudesDuplicadas = new AtomicLong();
    private final AtomicLong milisegundosAcumulados = new AtomicLong();
    private final Map<EstadoRespuesta, AtomicLong> porEstado = new ConcurrentHashMap<>();
    private final Set<String> rechazosAvisados = ConcurrentHashMap.newKeySet();

    /**
     * @param archivoBitacora     archivo de la bitácora (null = solo en memoria)
     * @param msConexionProveedor tiempo máximo para conectarse a un proveedor
     * @param msLecturaProveedor  tiempo máximo esperando la respuesta de un proveedor
     * @param reintentosProveedor reintentos limitados tras un fallo de red con un proveedor
     * @param maximoFallos        fallos consecutivos tras los cuales un proveedor se retira del registro
     * @param token               secreto compartido para registrar proveedores (null o vacío = sin autenticación)
     */
    public NucleoBroker(Path archivoBitacora, int msConexionProveedor, int msLecturaProveedor,
                        int reintentosProveedor, int maximoFallos, String token) {
        this.bitacora = new Bitacora(archivoBitacora);
        this.msConexionProveedor = msConexionProveedor;
        this.msLecturaProveedor = msLecturaProveedor;
        this.reintentosProveedor = reintentosProveedor;
        this.maximoFallos = Math.max(1, maximoFallos);
        this.token = token == null || token.isBlank() ? null : token;
        this.proxy = new ProxyHttp(registro, bitacora, msConexionProveedor, msLecturaProveedor);
        bitacora.evento("Broker HTTP iniciado");
        Consola.broker("Broker HTTP listo (timeout proveedor " + msLecturaProveedor + " ms, reintentos "
                + reintentosProveedor + ", autenticacion " + (this.token == null ? "desactivada" : "activada") + ")");
    }

    public RegistroServicios getRegistro() {
        return registro;
    }

    public Bitacora getBitacora() {
        return bitacora;
    }

    public ProxyHttp getProxy() {
        return proxy;
    }

    // ------------------------------------------------------------------ health check

    /**
     * Respuesta del health check de Render ({@code GET /api/broker/health}): no toca la base de datos ni
     * contacta a los proveedores, solo confirma que el proceso está vivo.
     */
    // REFACTOR: Extract Function
    // RAZON: con sockets la unica forma de saber si el Broker vivia era pedir ESTADO, que arma metricas y lista de
    // solicitudes. Render necesita un health check barato y sin base de datos: se separo en un metodo propio que
    // solo confirma que el proceso responde.
    public Map<String, Object> salud() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("componente", "broker");
        m.put("transporte", "HTTP");
        m.put("serviciosRegistrados", registro.listarNombres().size());
        m.put("hora", LocalDateTime.now());
        return m;
    }

    // ------------------------------------------------------------------ autenticación

    /** true si no hay secreto configurado o el enviado coincide (comparación en tiempo constante). */
    // REFACTOR: Extract Function
    // RAZON: la decision 'este llamador conoce el secreto' se necesita en el registro, la baja y los proveedores.
    // Un metodo unico, con comparacion en tiempo constante ({@code MessageDigest.isEqual}), evita repetir la
    // comprobacion y hace que el token sea opcional (sin BROKER_TOKEN todo se acepta). El protocolo de sockets no
    // tenia ninguna autenticacion.
    public boolean autorizado(String enviado) {
        if (token == null) {
            return true;
        }
        if (enviado == null) {
            return false;
        }
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), enviado.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ solicitudes

    /**
     * Procesa el cuerpo JSON de {@code POST /api/broker/solicitar}: lo convierte en {@link Solicitud},
     * la enruta y devuelve SIEMPRE una {@link Respuesta} (los errores van dentro, con su estado).
     */
    // REFACTOR: Split Phase
    // RAZON: {@code procesarSolicitud} hacia dos cosas seguidas: leer y validar el mensaje, y enrutarlo. Ahora la
    // fase 1 (unmarshalling + rechazo si esta mal formado) vive aqui y la fase 2 (enrutar) es {@code
    // solicitar(Solicitud, String)}, que ya recibe un objeto valido.
    public Respuesta solicitar(Map<String, Object> json, String origen) {
        Solicitud solicitud;
        try {
            solicitud = Marshalling.unmarshalSolicitud(json);                       // unmarshalling
        } catch (IllegalArgumentException e) {
            Object id = json == null ? null : json.get("idSolicitud");
            return rechazar(id instanceof String texto && !texto.isBlank() ? texto : "sin-id",
                    "Solicitud mal formada: " + e.getMessage(), origen);
        }
        return solicitar(solicitud, origen);
    }

    /** Enruta una solicitud ya construida (por ejemplo, la que arma {@code POST /api/broker/{servicio}/{operacion}}). */
    // REFACTOR: Change Function Declaration
    // RAZON: antes devolvia un String con la respuesta ya convertida a JSON (porque debia escribirla en el
    // socket). Ahora devuelve la {@link Respuesta} y el marshalling lo hace quien responde por HTTP; asi el nucleo
    // no sabe como viaja la respuesta y tambien lo usa {@code BrokerController} sin convertir ida y vuelta.
    public Respuesta solicitar(Solicitud solicitud, String origen) {
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
        return respuesta;
    }

    // REFACTOR: Change Function Declaration
    // RAZON: {@code rechazarSolicitud} devolvia el JSON ya escrito; ahora devuelve una Respuesta (estado
    // SOLICITUD_INVALIDA) y sigue contando y anotando el rechazo en la bitacora.
    private Respuesta rechazar(String id, String mensaje, String origen) {
        Consola.broker("Solicitud rechazada (" + origen + "): " + mensaje);
        contar(EstadoRespuesta.SOLICITUD_INVALIDA, 0);
        bitacora.registrar(Consola.corto(id), "?", "?", EstadoRespuesta.SOLICITUD_INVALIDA, 0, mensaje);
        return Respuesta.error(id, EstadoRespuesta.SOLICITUD_INVALIDA, mensaje);
    }

    private void contar(EstadoRespuesta estado, long ms) {
        solicitudesTotales.incrementAndGet();
        milisegundosAcumulados.addAndGet(ms);
        porEstado.computeIfAbsent(estado, k -> new AtomicLong()).incrementAndGet();
    }

    /** Si un proveedor falla varias veces seguidas se retira del registro (puede volver a registrarse). */
    private void vigilarProveedor(String servicio, Respuesta respuesta) {
        EstadoRespuesta estado = respuesta.getEstado();
        if (estado != EstadoRespuesta.PROVEEDOR_NO_DISPONIBLE && estado != EstadoRespuesta.TIEMPO_AGOTADO) {
            return;
        }
        registro.buscar(servicio).ifPresent(p -> {
            if (p instanceof ProveedorHttp remoto && remoto.getFallosConsecutivos() >= maximoFallos
                    && registro.eliminar(servicio, remoto)) {
                Consola.broker("Proveedor '" + servicio + "' (" + remoto.destino() + ") retirado tras "
                        + remoto.getFallosConsecutivos() + " fallos consecutivos; volvera cuando se registre de nuevo");
                bitacora.evento("Proveedor retirado por fallos: " + servicio + " @ " + remoto.destino());
            }
        });
    }

    // ------------------------------------------------------------------ registro dinámico

    /**
     * Registro dinámico por HTTP ({@code POST /api/broker/register}). Cubre cinco casos: alta nueva, latido del
     * mismo proveedor, actualización de sus operaciones, sustitución explícita y rechazo por conflicto.
     *
     * @param tokenEnviado valor de la cabecera X-Broker-Token
     * @param json         cuerpo ya convertido a mapa
     * @param origen       dirección de quien llama, solo para la bitácora
     */
    // REFACTOR: Change Function Declaration
    // RAZON: {@code registrarProveedor(Map)} recibia host y puerto TCP y devolvia un String JSON. Ahora recibe el
    // token, el mapa y el origen y devuelve un {@link Acuse} (200 alta/latido/sustitucion, 400 invalido, 401 sin
    // token, 409 conflicto). Los cinco casos de decision (alta, latido, actualizacion, sustitucion, rechazo) se
    // conservaron tal cual; solo cambia como se informa el resultado.
    public Acuse registrar(String tokenEnviado, Map<String, Object> json, String origen) {
        if (!autorizado(tokenEnviado)) {
            Consola.registro("REGISTRO RECHAZADO (" + origen + "): token invalido");
            return new Acuse(401, false, "No autorizado: falta o es incorrecto el token del Broker");
        }
        RegistroProveedor datos;
        try {
            datos = RegistroProveedor.desdeMapa(json);                             // fase 1: parsear y validar
        } catch (IllegalArgumentException e) {
            Consola.registro("REGISTRO RECHAZADO (" + origen + "): " + e.getMessage());
            return new Acuse(400, false, "Registro rechazado: " + e.getMessage());
        }

        synchronized (registro) {                                                  // fase 2: decidir
            ProveedorHttp nuevo = new ProveedorHttp(datos, msConexionProveedor, msLecturaProveedor,
                    reintentosProveedor, token);
            Optional<ProveedorServicio> actual = registro.buscar(datos.servicio());

            if (actual.isEmpty()) {
                registro.registrar(datos.servicio(), nuevo);
                Consola.registro("Proveedor registrado via HTTP desde " + origen + ": " + datos.servicio()
                        + " -> " + nuevo.destino());
                bitacora.evento("Proveedor registrado via HTTP: " + datos.servicio() + " @ " + nuevo.destino()
                        + " " + new TreeSet<>(datos.operaciones()));
                return new Acuse(200, true, "Proveedor '" + datos.servicio() + "' registrado");
            }
            if (actual.get() instanceof ProveedorHttp existente && existente.mismoDestino(datos.url())) {
                // El mismo proveedor avisando de nuevo (latido o reinicio): no se hace ruido en consola
                if (existente.mismasOperaciones(datos.operaciones()) && existente.getRutaBase().equals(datos.rutaBase())) {
                    existente.tocar();
                } else {
                    registro.sustituir(datos.servicio(), nuevo);
                    bitacora.evento("Proveedor actualizado: " + datos.servicio() + " @ " + nuevo.destino());
                }
                return new Acuse(200, true, "Proveedor '" + datos.servicio() + "' ya estaba registrado");
            }
            if (datos.reemplazar()) {
                registro.sustituir(datos.servicio(), nuevo);
                bitacora.evento("Proveedor SUSTITUIDO: " + datos.servicio() + " ahora @ " + nuevo.destino());
                return new Acuse(200, true, "Proveedor '" + datos.servicio() + "' sustituido");
            }
            String clave = datos.servicio() + "@" + datos.url();
            if (rechazosAvisados.add(clave)) {
                Consola.registro("REGISTRO RECHAZADO: '" + datos.servicio() + "' ya tiene otro proveedor ("
                        + actual.get() + "); " + datos.url() + " debe pedir reemplazar=true");
            }
            return new Acuse(409, false, "Ya existe un proveedor registrado para '" + datos.servicio()
                    + "'; usa reemplazar para sustituirlo");
        }
    }

    /** Baja de un proveedor ({@code POST /api/broker/unregister}); solo la acepta quien está registrado con esa URL. */
    // REFACTOR: Change Function Declaration
    // RAZON: la baja anterior ({@code darDeBaja(Map)}) aceptaba un host:puerto y respondia un String. Ahora valida
    // el token y la URL, solo retira al proveedor que se registro con esa misma URL (nadie puede dar de baja a
    // otro) y responde con un Acuse (200/400/401/404).
    public Acuse darDeBaja(String tokenEnviado, Map<String, Object> json, String origen) {
        if (!autorizado(tokenEnviado)) {
            return new Acuse(401, false, "No autorizado: falta o es incorrecto el token del Broker");
        }
        String servicio = json != null && json.get("servicio") instanceof String s && !s.isBlank() ? s.trim() : null;
        String urlTexto = json != null && json.get("url") instanceof String u && !u.isBlank() ? u : null;
        if (servicio == null || urlTexto == null) {
            return new Acuse(400, false, "Baja rechazada: faltan servicio o url");
        }
        java.net.URI url;
        try {
            url = RegistroProveedor.urlBase(urlTexto);
        } catch (IllegalArgumentException e) {
            return new Acuse(400, false, "Baja rechazada: " + e.getMessage());
        }
        synchronized (registro) {
            Optional<ProveedorServicio> actual = registro.buscar(servicio);
            if (actual.isPresent() && actual.get() instanceof ProveedorHttp remoto
                    && remoto.mismoDestino(url) && registro.eliminar(servicio, remoto)) {
                bitacora.evento("Proveedor dado de baja: " + servicio + " @ " + remoto.destino() + " (origen " + origen + ")");
                return new Acuse(200, true, "Proveedor '" + servicio + "' dado de baja");
            }
        }
        return new Acuse(404, false, "No habia un proveedor '" + servicio + "' registrado desde esa url");
    }

    // ------------------------------------------------------------------ monitoreo

    /** Foto del estado del Broker: servicios registrados, métricas y últimas solicitudes. */
    // REFACTOR: Remove Dead Code
    // RAZON: el estado devolvia los campos {@code tipo} (ESTADO) y {@code puerto}, que solo tenian sentido para el
    // protocolo de sockets. Ahora informa {@code transporte: HTTP} y, por servicio, su url, ruta base, fallos y
    // ultimo contacto; el resto de las metricas no cambio.
    public Map<String, Object> estado() {
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("transporte", "HTTP");
        raiz.put("iniciadoEn", iniciadoEn);

        List<Map<String, Object>> servicios = new ArrayList<>();
        for (String nombre : registro.listarNombres()) {
            registro.buscar(nombre).ifPresent(p -> {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("servicio", nombre);
                s.put("operaciones", new TreeSet<>(p.getOperacionesSoportadas()));
                if (p instanceof ProveedorHttp remoto) {
                    s.put("tipo", "remoto");
                    s.put("url", remoto.destino());
                    s.put("rutaBase", remoto.getRutaBase());
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
