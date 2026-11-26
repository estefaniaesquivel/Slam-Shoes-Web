package com.slamshoes.broker.http;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.ProveedorServicio;

/**
 * Auto-registro de un proveedor en el Broker por HTTP: al arrancar envía {@code POST {BROKER_URL}/api/broker/register}
 * con su nombre de servicio y su URL pública ({@code SERVICE_PUBLIC_URL}), y repite el aviso cada cierto tiempo (latido).
 *
 * El latido es importante en la nube: el directorio del Broker vive en memoria, así que si el Broker se reinicia o
 * se vuelve a desplegar en Render, los proveedores vuelven a aparecer solos en pocos segundos, sin intervención.
 * Al detenerse el proveedor se da de baja ({@code POST /api/broker/unregister}).
 *
 * El proveedor se anuncia con la URL PÚBLICA, no con el puerto interno: Render asigna el puerto en {@code $PORT} y lo
 * expone por HTTPS, así que el registro funciona sin importar el puerto real del contenedor.
 *
 * REFACTOR: Extract Class y Substitute Algorithm
 * RAZÓN: el registro vivía dentro de {@code ProveedorServidor} junto al ServerSocket, y registraba un host:puerto TCP
 * que Render no deja alcanzar. Aislado aquí, hace lo mismo con un POST HTTP a una URL pública y se puede probar sin
 * ningún servidor propio.
 */
public class RegistroAutomatico {

    private final ProveedorServicio proveedor;
    private final String nombre;
    private final URI urlPublica;
    private final String rutaBase;
    private final URI urlBroker;
    private final String token;
    private final boolean reemplazar;
    private final long msLatido;

    private volatile boolean activo = false;
    private volatile boolean registrado = false;
    private Thread hilo;

    // Solo para no repetir el mismo mensaje en consola
    private boolean brokerCaidoAvisado = false;
    private boolean rechazoAvisado = false;

    /**
     * @param proveedor  la lógica de negocio que se anuncia
     * @param urlPublica URL con la que el Broker alcanza a este proveedor ({@code SERVICE_PUBLIC_URL})
     * @param rutaBase   raíz REST del servicio para el proxy /api/gateway (por ejemplo {@code /api/productos}; puede ser "")
     * @param urlBroker  URL del Broker ({@code BROKER_URL})
     * @param token      secreto compartido (null o vacío = sin autenticación)
     * @param reemplazar si es true y ya hay otro proveedor con ese nombre, lo sustituye
     * @param msLatido   cada cuántos milisegundos repite el aviso
     * @throws IllegalArgumentException si alguna de las dos URL no es http/https válida
     */
    public RegistroAutomatico(ProveedorServicio proveedor, String urlPublica, String rutaBase, String urlBroker,
                              String token, boolean reemplazar, long msLatido) {
        this.proveedor = Objects.requireNonNull(proveedor);
        this.nombre = proveedor.getNombreServicio();
        this.urlPublica = RegistroProveedor.urlBase(urlPublica);
        this.rutaBase = rutaBase == null ? "" : rutaBase;
        this.urlBroker = RegistroProveedor.urlBase(urlBroker);
        this.token = token == null || token.isBlank() ? null : token;
        this.reemplazar = reemplazar;
        this.msLatido = Math.max(200, msLatido);
    }

    /** Empieza a registrarse (en un hilo propio, para no frenar el arranque de la aplicación). */
    public synchronized void iniciar() {
        if (activo) {
            return;
        }
        activo = true;
        hilo = new Thread(this::mantenerRegistro, "registro-" + nombre);
        hilo.setDaemon(true);
        hilo.start();
        Consola.proveedor(nombre, "Anunciandose en el Broker " + urlBroker + " como " + urlPublica);
    }

    /** Se da de baja en el Broker (si puede) y deja de repetir el aviso. */
    // REFACTOR: Change Function Declaration
    // RAZON: {@code ProveedorServidor.detener()} cerraba el ServerSocket y avisaba la baja por el mismo socket.
    // Ahora solo envia {@code POST /api/broker/unregister}: no hay socket que cerrar ni hilos del pool que
    // esperar.
    public synchronized void detener() {
        if (!activo) {
            return;
        }
        activo = false;
        if (hilo != null) {
            hilo.interrupt();
        }
        try {
            String cuerpo = Json.escribir(Map.of("servicio", nombre, "url", urlPublica.toString()));
            MensajeHttp m = TransporteHttp.postJson(URI.create(urlBroker + RutasBroker.BAJA), cuerpo, cabeceras(), 1000, 1500);
            Consola.proveedor(nombre, "Baja en el Broker: HTTP " + m.codigo());
        } catch (IOException | RuntimeException e) {
            Consola.proveedor(nombre, "No se pudo avisar la baja al Broker (" + e.getClass().getSimpleName() + ")");
        }
        registrado = false;
    }

    public boolean estaRegistrado() {
        return registrado;
    }

    /**
     * Un intento de registro (lo usa el hilo y las pruebas).
     *
     * @param pedirReemplazo si es true pide sustituir a otro proveedor del mismo servicio
     * @return true si el Broker lo aceptó
     */
    // REFACTOR: Change Function Declaration
    // RAZON: {@code ProveedorServidor.registrarEnBroker(boolean)} era privado y devolvia void: nadie sabia si el
    // Broker acepto. Ahora es publico y devuelve true/false (HTTP 200 o no), lo que permite cortar el reintento
    // cuando ya se registro y probar el registro sin hilos.
    public boolean registrarUnaVez(boolean pedirReemplazo) {
        RegistroProveedor datos = new RegistroProveedor(nombre, urlPublica, rutaBase,
                new java.util.TreeSet<>(proveedor.getOperacionesSoportadas()), pedirReemplazo);
        try {
            MensajeHttp m = TransporteHttp.postJson(URI.create(urlBroker + RutasBroker.REGISTRAR),
                    Json.escribir(datos.aMapa()), cabeceras(), 1500, 3000);
            boolean ok = m.esCorrecto();
            if (ok && !registrado) {
                Consola.proveedor(nombre, "Registrado en el Broker " + urlBroker + " via HTTP (" + m.codigo() + ")");
            } else if (!ok && !rechazoAvisado) {
                Consola.proveedor(nombre, "El Broker rechazo el registro: HTTP " + m.codigo() + " " + m.cuerpo());
                rechazoAvisado = true;
            }
            registrado = ok;
            if (ok) {
                rechazoAvisado = false;
            }
            brokerCaidoAvisado = false;
            return ok;
        } catch (IOException | RuntimeException e) {
            if (registrado || !brokerCaidoAvisado) {
                Consola.proveedor(nombre, "No se pudo contactar al Broker (" + urlBroker + "); se reintentara automaticamente");
            }
            registrado = false;
            brokerCaidoAvisado = true;
            return false;
        }
    }

    // REFACTOR: Substitute Algorithm
    // RAZON: el latido anterior repetia el mensaje REGISTRAR cada 5 s hacia un host:puerto fijo. Ahora reintenta
    // cada 1 s hasta lograr el primer registro (Render puede tardar en despertar al Broker) y luego envia el
    // latido a la frecuencia configurada (BROKER_LATIDO_MS, 15 s por omision), con el mismo objetivo: que el
    // proveedor reaparezca solo si el Broker se reinicia.
    private void mantenerRegistro() {
        boolean primero = true;
        while (activo) {
            registrarUnaVez(primero && reemplazar);
            primero = primero && !registrado;
            try {
                // Mientras no logre registrarse intenta cada segundo; ya registrado, cada msLatido
                Thread.sleep(registrado ? msLatido : Math.min(1000, msLatido));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private Map<String, String> cabeceras() {
        return token == null ? null : Map.of(RutasBroker.CABECERA_TOKEN, token);
    }
}
