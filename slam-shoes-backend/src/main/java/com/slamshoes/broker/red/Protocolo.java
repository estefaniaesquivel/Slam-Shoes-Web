package com.slamshoes.broker.red;

/**
 * Vocabulario del protocolo entre Cliente, Broker y Proveedores.
 *
 * Cada mensaje es UN objeto JSON en UNA línea (terminada en \n) por un socket TCP, en UTF-8.
 * El campo "tipo" dice de qué se trata:
 *
 * <pre>
 *  Cliente  -> Broker     SOLICITUD   pide una operación           (el Broker contesta RESPUESTA)
 *  Broker   -> Proveedor  SOLICITUD   reenvía la operación         (el Proveedor contesta RESPUESTA)
 *  Proveedor-> Broker     REGISTRAR   "yo atiendo este servicio"   (el Broker contesta ACK)
 *  Proveedor-> Broker     BAJA        "me voy"                     (el Broker contesta ACK)
 *  Cualquiera-> Broker    ESTADO      monitoreo                    (el Broker contesta ESTADO)
 * </pre>
 */
public final class Protocolo {

    public static final String TIPO = "tipo";

    public static final String SOLICITUD = "SOLICITUD";
    public static final String RESPUESTA = "RESPUESTA";
    public static final String REGISTRAR = "REGISTRAR";
    public static final String BAJA = "BAJA";
    public static final String ESTADO = "ESTADO";
    public static final String ACK = "ACK";

    /** Puerto por omisión del Broker. */
    public static final int PUERTO_BROKER = 9000;

    /** Dirección en la que escuchan los servidores. Por seguridad, solo esta máquina (loopback). */
    public static String direccionEscucha() {
        return System.getProperty("slam.bind", "127.0.0.1");
    }

    private Protocolo() {
    }
}
