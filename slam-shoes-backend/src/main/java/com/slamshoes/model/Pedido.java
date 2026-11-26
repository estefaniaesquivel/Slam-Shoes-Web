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
    // RAZON: Antes era un String libre ("PENDIENTE", "CONCRETADO") con valores mágicos repetidos en varias clases.
    // Ahora es EstadoPedido; @Enumerated(STRING) guarda el mismo texto de siempre, así la base de datos no cambia.
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(name = "direccion_envio")
    private String direccionEnvio;

    @Column(name = "metodo_pago")
    private String metodoPago;

    public Pedido() {}

    // Getters y Setters
    public Long getIdPedido() { return idPedido; }
    public void setIdPedido(Long idPedido) { this.idPedido = idPedido; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    /*REFACTOR: Remove Setting Method
    RAZON: Se eliminó setEstado para impedir cambios arbitrarios desde otras clases. El estado inicia como PENDIENTE y solo se modifica mediante concretar(), reabrir() o marcarNoConcretado().
    */
    public EstadoPedido getEstado() { return estado; }

    public void concretar() {
        exigirEstado(EstadoPedido.PENDIENTE,
                "El pedido #" + idPedido + " no está en proceso (estado actual: " + estado + ")");
        estado = EstadoPedido.CONCRETADO;
    }

    public void reabrir() {
        exigirEstado(EstadoPedido.CONCRETADO,
                "El pedido #" + idPedido + " no está concretado (estado actual: " + estado + ")");
        estado = EstadoPedido.PENDIENTE;
    }

    public void marcarNoConcretado() {
        exigirEstado(EstadoPedido.PENDIENTE,
                "Solo se pueden eliminar pedidos en proceso. El pedido #" + idPedido + " está " + estado);
        estado = EstadoPedido.NO_CONCRETADO;
    }

    private void exigirEstado(EstadoPedido esperado, String mensaje) {
        if (estado != esperado) {
            throw new TransicionEstadoInvalidaException(mensaje);
        }
    }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}