package com.slamshoes.model.enums;

import com.slamshoes.exception.ValidacionException;

// REFACTOR: Replace Type Code with Class (enum)
// RAZON: Antes Pedido.estado era un String libre y los valores "PENDIENTE" y "CONCRETADO" estaban repetidos como
// cadenas mágicas en el proveedor y en los controladores; un error de dedo ("Pendiente") compilaba sin problema.
// Ahora los estados posibles son un enum cerrado y el compilador rechaza cualquier valor inventado.
/**
 * Estados posibles de un pedido (RF-16, RF-27 a RF-30).
 * <ul>
 *   <li>{@link #PENDIENTE}: en proceso, el administrador aún no lo entrega.</li>
 *   <li>{@link #CONCRETADO}: entregado y cerrado.</li>
 *   <li>{@link #NO_CONCRETADO}: el administrador lo descartó; el pedido se elimina y el stock regresa al inventario,
 *       por eso este valor nunca queda guardado en la base de datos.</li>
 * </ul>
 */
public enum EstadoPedido {
    PENDIENTE,
    CONCRETADO,
    NO_CONCRETADO;

    /**
     * Convierte el texto del panel del administrador en uno de los estados que se pueden listar.
     *
     * @param texto valor recibido en la petición ({@code PENDIENTE} o {@code CONCRETADO}, sin importar mayúsculas)
     * @return el estado correspondiente
     * @throws ValidacionException si el texto es nulo o no es un estado listable
     */
    public static EstadoPedido paraListado(String texto) {
        String limpio = texto == null ? "" : texto.trim().toUpperCase();
        if (limpio.equals(PENDIENTE.name())) return PENDIENTE;
        if (limpio.equals(CONCRETADO.name())) return CONCRETADO;
        throw new ValidacionException("El parametro 'estado' debe ser PENDIENTE o CONCRETADO");
    }
}
