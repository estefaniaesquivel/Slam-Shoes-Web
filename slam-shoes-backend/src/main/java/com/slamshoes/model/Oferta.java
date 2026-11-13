package com.slamshoes.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ofertas")
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_oferta")
    private Long idOferta;

    @Column(name = "id_producto")
    private Long idProducto;

    private String nombre; // opcional, ej. 'BUEN FIN'

    @Column(name = "porcentaje_descuento")
    private Double porcentajeDescuento;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    private Boolean activa;

    public Oferta() {}

    public Long getIdOferta() { return idOferta; }
    public void setIdOferta(Long idOferta) { this.idOferta = idOferta; }
    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Double getPorcentajeDescuento() { return porcentajeDescuento; }
    public void setPorcentajeDescuento(Double porcentajeDescuento) { this.porcentajeDescuento = porcentajeDescuento; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
