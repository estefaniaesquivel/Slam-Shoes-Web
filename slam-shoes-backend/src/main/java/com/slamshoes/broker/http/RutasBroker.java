package com.slamshoes.broker.http;

/**
 * Vocabulario HTTP del Broker: todas las rutas y cabeceras en UN solo lugar.
 *
 * <pre>
 *  Proveedor -> Broker     POST /api/broker/register      "yo atiendo este servicio"  (registro dinámico)
 *  Proveedor -> Broker     POST /api/broker/unregister    "me voy"                    (baja)
 *  Render    -> Broker     GET  /api/broker/health        "¿sigues vivo?"             (health check)
 *  Cliente   -> Broker     POST /api/broker/solicitar     envoltura Solicitud/Respuesta (patrón Broker)
 *  Cliente   -> Broker     ANY  /api/gateway/{servicio}/** reenvío HTTP transparente (proxy)
 *  Broker    -> Proveedor  POST /api/proveedor/{servicio} reenvía la Solicitud al proveedor
 * </pre>
 *
 * REFACTOR: Replace Magic Literal
 * RAZÓN: antes el protocolo de sockets repartía textos como "REGISTRAR", "BAJA" o el puerto 9000 entre
 * varias clases; un error de dedo en cualquiera rompía la comunicación sin avisar al compilador.
 * Con las rutas como constantes nombradas, el cliente, el Broker y los proveedores comparten la misma
 * definición y un cambio de ruta se hace en un único sitio.
 */
public final class RutasBroker {

    /** Prefijo de todo lo que el Broker expone. */
    public static final String BASE = "/api/broker";

    /** Alta de un proveedor (cuerpo JSON: servicio, url, ...). */
    public static final String REGISTRAR = BASE + "/register";

    /** Baja de un proveedor. */
    public static final String BAJA = BASE + "/unregister";

    /** Health check ligero para Render (sin tocar la base de datos). */
    public static final String SALUD = BASE + "/health";

    /** Monitoreo: servicios registrados, métricas y últimas solicitudes. */
    public static final String ESTADO = BASE + "/estado";

    /** Envío de una {@link com.slamshoes.broker.Solicitud} completa al Broker. */
    public static final String SOLICITAR = BASE + "/solicitar";

    /** Prefijo del reenvío HTTP transparente: /api/gateway/{servicio}/{ruta...}. */
    public static final String PROXY = "/api/gateway";

    /** Prefijo con el que cada proveedor recibe las solicitudes: /api/proveedor/{servicio}. */
    public static final String PROVEEDOR = "/api/proveedor";

    /** Cabecera con el secreto compartido entre Broker y proveedores (opcional, variable BROKER_TOKEN). */
    public static final String CABECERA_TOKEN = "X-Broker-Token";

    private RutasBroker() {
    }

    /** Ruta completa donde un proveedor atiende las solicitudes de su servicio. */
    public static String rutaProveedor(String servicio) {
        return PROVEEDOR + "/" + servicio;
    }
}
