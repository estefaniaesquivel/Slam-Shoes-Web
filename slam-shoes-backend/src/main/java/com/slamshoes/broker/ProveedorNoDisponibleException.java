package com.slamshoes.broker;

/**
 * Se lanza cuando un proveedor REMOTO está registrado pero no se pudo obtener su respuesta
 * (conexión rechazada, desconexión o tiempo agotado) después de agotar los reintentos.
 *
 * {@link BrokerCentral} la convierte en una {@link Respuesta} con estado
 * PROVEEDOR_NO_DISPONIBLE o TIEMPO_AGOTADO; nunca llega al cliente como excepción.
 */
public class ProveedorNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean tiempoAgotado;

    public ProveedorNoDisponibleException(String mensaje, boolean tiempoAgotado, Throwable causa) {
        super(mensaje, causa);
        this.tiempoAgotado = tiempoAgotado;
    }

    /** true si el motivo fue que el proveedor no respondió a tiempo (y no que rechazó la conexión). */
    public boolean isTiempoAgotado() {
        return tiempoAgotado;
    }
}
