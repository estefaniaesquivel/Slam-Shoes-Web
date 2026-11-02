package com.slamshoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "carrito_detalle")
public class CarritoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrito_detalle")
    private Long idCarritoDetalle;

    @Column(name = "id_carrito")
    private Long idCarrito;

    @Column(name = "id_variante")
    private Long idVariante;

    private Integer cantidad;

    // Getters y Setters
    public Long getIdCarritoDetalle() { return idCarritoDetalle; }
    public void setIdCarritoDetalle(Long idCarritoDetalle) { this.idCarritoDetalle = idCarritoDetalle; }
    public Long getIdCarrito() { return idCarrito; }
    public void setIdCarrito(Long idCarrito) { this.idCarrito = idCarrito; }
    public Long getIdVariante() { return idVariante; }
    public void setIdVariante(Long idVariante) { this.idVariante = idVariante; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}