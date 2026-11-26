package com.slamshoes.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.slamshoes.exception.TransicionEstadoInvalidaException;
import com.slamshoes.model.enums.EstadoPedido;

/**
 * Pedido de un cliente (RF-16, RF-17, RF-27 a RF-30). Controla por sí mismo sus cambios de estado
 * ({@Link #concretar()}, {@Link #reabrir()}, {@Link #marcarNoConcretado()}).
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;

    @Column(name = "id_usuario")
    private Long idUsuario;

    private LocalDateTime fecha;
    private Double total;
    
    // REFACTOR: Replace Type Code with Class (enum)
    // RAZÓN: Antes era un String libre ("PENDIENTE", "CONCRETADO") con valores mágicos repetidos en varias clases.
    // Ahora es EstadoPedido; @Enumerated(STRING) guarda el mismo texto de siempre, así la base de datos no cambia.
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(name = "direccion_envio")
    private String direccionEnvio;

    @Column(name = "metodo_pago")
    private String metodoPago;

    /** Constructor vacío requerido por JPA. Para crear pedidos nuevos usar {@Link #crearPendiente()}. */
    public Pedido() {}

    // REFACTOR: Replace Constructor with Factory Method
    // RAZÓN: Antes quien creaba un Pedido tenía que acordarse de llamar setEstado("PENDIENTE") y setFecha(now),
    // olvidaro dejaba un pedido sin estado. Ahora este método con nombre deja siempre el estado en PENDIENTE.
    /**
     * Crea un pedido nuevo en estado ({@Link EstadoPedido#PENDIENTE}) con la fecha actual.
     * 
     * @param idUsuario      cliente que compra
     * @param total          total ya calculado (con ofertas)
     * @param direccionEnvio dirección de entrega
     * @param metodoPago     método de pago, o {@code null} si no se indicó
     * @return pedido listo para guardar
     */
    public static Pedido crearPendiente(Long idUsuario, Double total, String direccionEnvio, String metodoPago) {
        Pedido p = new Pedido();
        p.idUsuario = idUsuario;
        p.fecha = LocalDateTime.now();
        p.total = total;
        p.estado = EstadoPedido.PENDIENTE;
        p.direccionEnvio = direccionEnvio;
        p.metodoPago = metodoPago;
        return p;
    }

    // REFACTOR: Move Method
    // RAZÓN: Antes la regla "solo se concreta un pedido en proceso" estaba en ServicioPedidosJpa.cambiarEstado,
    // comparando cadenas desde afuera. Ahora la conoce el propio Pedido, que es el dueño del dato.
    /**
     * Pasa el pedido PENDIENTE a CONCRETADO
     * 
     * @throws TransicionEstadoInvalidaException si el pedido no está en proceso
     */
    public void concretar() {
        if (estado != EstadoPedido.PENDIENTE) {
            throw new TransicionEstadoInvalidaException(
                "El pedido #" + idPedido + " no está en proceso (estado actual: " + estado + ")");
        }
        estado = EstadoPedido.CONCRETADO;
    }

    /**
     * Regresa el pedido de CONCRETADO a PENDIENTE (corrige un error de dedo).
     * 
     * @throws TransicionEstadoInvalidaException si el pedido no está concretado
     */
    public void reabrir() {
        if (estado != EstadoPedido.CONCRETADO) {
            throw new TransicionEstadoInvalidaException(
                "El pedido #" + idPedido + " no está concretado (estado actual: " + estado + ")");
        }
        estado = EstadoPedido.PENDIENTE;
    }

    /**
     * Marca el pedido como NO_CONCRETADO. Sólo es válido si estaba en proceso, después el servicio lo elimina y regresa el stock.
     * 
     * @throws TransicionEstadoInvalidaException si el pedido no está en proceso
     */
    public void marcarNoConcretado() {
        if (estado != EstadoPedido.PENDIENTE) {
            throw new TransicionEstadoInvalidaException(
                "Solo se puede eliminar pedidos en proceso. El pedido #" + idPedido + " está " + estado);
        }
        estado = EstadoPedido.NO_CONCRETADO;
    }

    // Getters y Setters
    public Long getIdPedido() { return idPedido; }
    public void setIdPedido(Long idPedido) { this.idPedido = idPedido; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    // REFACTOR: Remove Setting Method
    // RAZON: Se eliminó setEstado para impedir cambios arbitrarios desde otras clases.
    // El estado inicia como PENDIENTE y solo se modifica mediante concretar(), reabrir() o marcarNoConcretado().
    public EstadoPedido getEstado() { return estado; }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}