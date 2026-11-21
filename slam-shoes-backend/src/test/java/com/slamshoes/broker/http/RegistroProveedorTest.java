package com.slamshoes.broker.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas del JSON de registro ({@code POST /api/broker/register}) convertido en {@link RegistroProveedor}. */
class RegistroProveedorTest {

    @Test
    @DisplayName("El JSON minimo del enunciado {servicio, url} es valido")
    void registroMinimo() {
        RegistroProveedor r = RegistroProveedor.desdeMapa(
                ServidorPrueba.mapa("servicio", "catalogo", "url", "https://slam-catalogo.onrender.com"));
        assertEquals("catalogo", r.servicio());
        assertEquals("https://slam-catalogo.onrender.com", r.url().toString());
        assertEquals("", r.rutaBase());
        assertTrue(r.operaciones().isEmpty());
        assertFalse(r.reemplazar());
    }

    @Test
    @DisplayName("La URL se normaliza: sin barra final y con el esquema en minusculas")
    void normalizaUrl() {
        RegistroProveedor r = RegistroProveedor.desdeMapa(
                ServidorPrueba.mapa("servicio", "pedidos", "url", "HTTPS://Mi-Servicio.onrender.com/"));
        assertEquals("https://Mi-Servicio.onrender.com", r.url().toString());
    }

    @Test
    @DisplayName("Acepta puerto explicito y operaciones, y normaliza la ruta base")
    void rutaBaseYOperaciones() {
        RegistroProveedor r = RegistroProveedor.desdeMapa(ServidorPrueba.mapa(
                "servicio", "productos", "url", "http://localhost:8081", "rutaBase", "api/productos/",
                "operaciones", List.of(" listarProductos ", "", "obtenerDetalle"), "reemplazar", true));
        assertEquals("http://localhost:8081", r.url().toString());
        assertEquals("/api/productos", r.rutaBase());
        assertEquals(2, r.operaciones().size());
        assertTrue(r.operaciones().contains("listarProductos"));
        assertTrue(r.reemplazar());
    }

    @Test
    @DisplayName("Rechaza datos invalidos con un mensaje claro")
    void rechazaInvalidos() {
        List<Map<String, Object>> malos = List.of(
                ServidorPrueba.mapa("url", "http://localhost:1"),                                   // sin servicio
                ServidorPrueba.mapa("servicio", "a b", "url", "http://localhost:1"),                 // nombre con espacio
                ServidorPrueba.mapa("servicio", "x"),                                                // sin url
                ServidorPrueba.mapa("servicio", "x", "url", "ftp://servidor"),                       // no es http
                ServidorPrueba.mapa("servicio", "x", "url", "http://usuario:clave@servidor"),        // credenciales
                ServidorPrueba.mapa("servicio", "x", "url", "http://servidor?a=1"),                  // parametros
                ServidorPrueba.mapa("servicio", "x", "url", "no es una url"),
                ServidorPrueba.mapa("servicio", "x", "url", "http://s", "rutaBase", "/api/../admin"),
                ServidorPrueba.mapa("servicio", "x", "url", "http://s", "operaciones", "listar"));    // no es lista
        for (Map<String, Object> malo : malos) {
            assertThrows(IllegalArgumentException.class, () -> RegistroProveedor.desdeMapa(malo), String.valueOf(malo));
        }
        assertThrows(IllegalArgumentException.class, () -> RegistroProveedor.desdeMapa(null));
    }

    @Test
    @DisplayName("aMapa() produce el mismo JSON que el broker sabe leer (ida y vuelta)")
    void idaYVuelta() {
        RegistroProveedor original = RegistroProveedor.desdeMapa(ServidorPrueba.mapa(
                "servicio", "usuarios", "url", "http://localhost:9", "rutaBase", "/api/usuarios",
                "operaciones", List.of("registrar", "iniciarSesion")));
        Map<String, Object> porElCable = Json.leerObjeto(Json.escribir(original.aMapa()));
        assertEquals(original, RegistroProveedor.desdeMapa(porElCable));
    }
}
