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

@Entity
@Table(name = "pedidos")
public class Pedido {

    public enum EstadoPedido {
        PENDIENTE,
        CONCRETADO
    }

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
    private EstadoPedido estado;

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

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public void transicionarA(EstadoPedido nuevoEstado) {
        if (nuevoEstado == null) {
            throw new TransicionEstadoInvalidaException("El estado de destino del pedido no es válido");
        }

        EstadoPedido esperado = nuevoEstado == EstadoPedido.CONCRETADO
                ? EstadoPedido.PENDIENTE
                : EstadoPedido.CONCRETADO;
        if (estado != esperado) {
            String estadoEsperado = esperado == EstadoPedido.PENDIENTE ? "en proceso" : "concretado";
            throw new TransicionEstadoInvalidaException("El pedido #" + idPedido + " no está " + estadoEsperado
                    + " (estado actual: " + estado + ")");
        }
        estado = nuevoEstado;
    }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}