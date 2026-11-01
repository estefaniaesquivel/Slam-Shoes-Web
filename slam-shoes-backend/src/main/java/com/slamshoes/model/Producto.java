package com.slamshoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    @Column(name = "id_categoria")
    private Long idCategoria;

    @Column(name = "id_jugador")
    private Long idJugador;

    @Column(name = "id_coleccion")
    private Long idColeccion; // Llave foránea hacia la tabla colecciones

    private String nombre;
    private String marca;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    private Double precio;

    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    private Boolean activo;

    private String modelo;

    @Column(name = "es_tendencia")
    private Boolean esTendencia;

    // Constructores
    public Producto() {}

    // Getters y Setters
    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }

    public Long getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Long idCategoria) { this.idCategoria = idCategoria; }

    public Long getIdJugador() { return idJugador; }
    public void setIdJugador(Long idJugador) { this.idJugador = idJugador; }

    public Long getIdColeccion() { return idColeccion; }
    public void setIdColeccion(Long idColeccion) { this.idColeccion = idColeccion; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Boolean getEsTendencia() { return esTendencia; }
    public void setEsTendencia(Boolean esTendencia) { this.esTendencia = esTendencia; }
}