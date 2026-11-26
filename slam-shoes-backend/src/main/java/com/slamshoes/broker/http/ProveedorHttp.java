package com.slamshoes.broker.http;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
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
 * Representante (proxy) de un proveedor que vive en OTRO servicio web, alcanzado por HTTP/HTTPS.
 *
 * Implementa la misma interfaz {@link ProveedorServicio} que los proveedores locales, así que
 * {@link com.slamshoes.broker.BrokerCentral} y {@link com.slamshoes.broker.RegistroServicios}
 * funcionan igual que siempre: no saben que "ejecutar" ahora es un {@code POST} a
 * {@code {url}/api/proveedor/{servicio}}.
 *
 * Hace el marshalling de la solicitud, la envía con timeouts y, si el servicio no contesta o responde
 * 502/503/504 (típico de un servicio de Render que está despertando), reintenta un número LIMITADO de veces
 * con el mismo idSolicitud (el proveedor descarta los duplicados).
 *
 * REFACTOR: Substitute Algorithm
 * RAZÓN: la versión anterior ({@code ProveedorRemoto}) ejecutaba abriendo un Socket TCP hacia host:puerto,
 * algo que el proxy inverso de Render no permite. Aquí el cuerpo del método sigue siendo el mismo ciclo
 * "marshal → enviar → unmarshal → reintentar" pero el envío es HTTP, por lo que BrokerCentral no se modificó.
 */
public class ProveedorHttp implements ProveedorServicio {

    private final String nombreServicio;
    private final URI url;
    private final String rutaBase;
    private final Set<String> operaciones;
    private final int msConexion;
    private final int msLectura;
    private final int reintentos;
    private final String token;

    private final AtomicInteger fallosConsecutivos = new AtomicInteger();
    private volatile LocalDateTime ultimoContacto = LocalDateTime.now();

    /**
     * @param registro   datos con los que el proveedor se registró
     * @param msConexion tiempo máximo para conectarse
     * @param msLectura  tiempo máximo esperando la respuesta
     * @param reintentos reintentos limitados tras un fallo de red o un 502/503/504
     * @param token      secreto compartido que se envía en la cabecera X-Broker-Token (null = no se envía)
     */
    // REFACTOR: Introduce Parameter Object
    // RAZON: el constructor anterior ({@code ProveedorRemoto}) recibia nombre, host, puerto y operaciones como
    // parametros sueltos. Ahora recibe el {@link RegistroProveedor} ya validado, que agrupa esos datos (y la url y
    // la ruta base), por lo que la lista de parametros es mas corta y no se pueden combinar valores
    // inconsistentes.
    public ProveedorHttp(RegistroProveedor registro, int msConexion, int msLectura, int reintentos, String token) {
        Objects.requireNonNull(registro);
        this.nombreServicio = registro.servicio();
        this.url = registro.url();
        this.rutaBase = registro.rutaBase();
        this.operaciones = new TreeSet<>(registro.operaciones());
        this.msConexion = msConexion;
        this.msLectura = msLectura;
        this.reintentos = Math.max(0, reintentos);
        this.token = token == null || token.isBlank() ? null : token;
    }

    @Override
    // REFACTOR: Decompose Conditional
    // RAZON: en la version por sockets el exito o fallo era 'se leyo una linea o no'. Con HTTP hay que distinguir
    // codigo 2xx, codigos transitorios (reintentar), errores definitivos 4xx (no reintentar, mensaje claro) y
    // cuerpo ilegible. Cada rama quedo separada y con nombre, y solo el fallo de red o 502/503/504/429 incrementa
    // los fallos consecutivos que retiran al proveedor.
    public Respuesta ejecutar(Solicitud solicitud) {
        String json = Marshalling.marshalSolicitud(solicitud);                  // marshalling
        URI destino = URI.create(url + RutasBroker.rutaProveedor(nombreServicio));
        Map<String, String> cabeceras = token == null ? null : Map.of(RutasBroker.CABECERA_TOKEN, token);
        int intentos = reintentos + 1;
        IOException ultimoError = null;
        String ultimoCodigo = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                MensajeHttp mensaje = TransporteHttp.postJson(destino, json, cabeceras, msConexion, msLectura);
                if (mensaje.esTransitorio()) {
                    ultimoError = null;
                    ultimoCodigo = "HTTP " + mensaje.codigo();
                    Consola.broker("Intento " + intento + "/" + intentos + " con " + destino() + " fallo: " + ultimoCodigo);
                } else if (!mensaje.esCorrecto()) {
                    // 401/403/404/500...: reintentar no cambia nada
                    fallosConsecutivos.incrementAndGet();
                    throw new IllegalStateException("El proveedor " + destino() + " respondio HTTP " + mensaje.codigo());
                } else {
                    Respuesta respuesta;
                    try {
                        respuesta = Marshalling.unmarshalRespuesta(Marshalling.leerMensaje(mensaje.cuerpo())); // unmarshalling
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Respuesta ilegible de " + destino() + ": " + e.getMessage());
                    }
                    fallosConsecutivos.set(0);
                    ultimoContacto = LocalDateTime.now();
                    return respuesta;
                }
            } catch (IOException e) {
                ultimoError = e;
                ultimoCodigo = null;
                Consola.broker("Intento " + intento + "/" + intentos + " con " + destino() + " fallo: " + describir(e));
            }
            if (intento < intentos) {
                try {
                    Thread.sleep(200L * intento); // espera creciente entre reintentos
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        fallosConsecutivos.incrementAndGet();
        boolean tiempo = ultimoError != null && TransporteHttp.esTiempoAgotado(ultimoError);
        throw new ProveedorNoDisponibleException(
                (tiempo ? "El proveedor '" + nombreServicio + "' no respondio a tiempo en "
                        : "No se pudo contactar al proveedor '" + nombreServicio + "' en ")
                        + destino() + " (" + intentos + " intento(s)" + (ultimoCodigo != null ? ", " + ultimoCodigo : "") + ")",
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

    /** URL base del proveedor (sin barra final). */
    public URI getUrl() {
        return url;
    }

    /** Ruta REST base usada por el proxy {@code /api/gateway/{servicio}/**}. */
    public String getRutaBase() {
        return rutaBase;
    }

    public String getToken() {
        return token;
    }

    public int getMsConexion() {
        return msConexion;
    }

    public int getMsLectura() {
        return msLectura;
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

    /** Registra un fallo ocurrido fuera de ejecutar (por ejemplo, al reenviar por el proxy). */
    public void registrarFallo() {
        fallosConsecutivos.incrementAndGet();
    }

    // REFACTOR: Change Function Declaration
    // RAZON: antes {@code mismoDestino(String host, int puerto)} comparaba dos valores; ahora {@code
    // mismoDestino(URI)} compara un solo objeto destino. El Broker lo usa para reconocer el latido del mismo
    // proveedor y distinguirlo de uno que intenta robarle el servicio.
    public boolean mismoDestino(URI otraUrl) {
        return url.equals(otraUrl);
    }

    public boolean mismasOperaciones(Set<String> otras) {
        return operaciones.equals(new TreeSet<>(otras));
    }

    public String destino() {
        return url.toString();
    }

    @Override
    public String toString() {
        return "ProveedorHttp[" + nombreServicio + " @ " + destino() + "]";
    }

    private static String describir(IOException e) {
        return TransporteHttp.esTiempoAgotado(e) ? "tiempo agotado"
                : e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
    }
}
