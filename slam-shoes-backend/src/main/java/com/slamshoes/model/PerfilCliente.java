package com.slamshoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfiles_cliente")
public class PerfilCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Long idPerfil;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(name = "metodo_pago")
    private String metodoPago;

    @Column(name = "talla_preferida")
    private String tallaPreferida;

    public PerfilCliente() {}

    public Long getIdPerfil() { return idPerfil; }
    public void setIdPerfil(Long idPerfil) { this.idPerfil = idPerfil; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public String getTallaPreferida() { return tallaPreferida; }
    public void setTallaPreferida(String tallaPreferida) { this.tallaPreferida = tallaPreferida; }
}
