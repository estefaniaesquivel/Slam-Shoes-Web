package com.slamshoes.dto;

// Datos del formulario público de registro. No incluye "rol" a propósito:
// aunque el cliente HTTP lo envíe en el JSON, se ignora.
public class RegistroDTO {
    private String nombre;
    private String correo;
    private String contrasena;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
