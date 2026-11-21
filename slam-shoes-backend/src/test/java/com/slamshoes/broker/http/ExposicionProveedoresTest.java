package com.slamshoes.broker.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/** Pruebas del lado proveedor: lo que pasa cuando llega {@code POST /api/proveedor/{servicio}} (sin red). */
class ExposicionProveedoresTest {

    private ProveedorFalso negocio;
    private ExposicionProveedores expo;

    @BeforeEach
    void preparar() {
        negocio = new ProveedorFalso("catalogo");
        expo = new ExposicionProveedores(List.of(negocio), "secreto");
    }

    private static String json(String operacion) {
        return Marshalling.marshalSolicitud(new Solicitud("catalogo", operacion, null));
    }

    @Test
    @DisplayName("Una solicitud valida se ejecuta y devuelve EXITO con el mismo id")
    void ejecuta() {
        Solicitud s = new Solicitud("catalogo", "listarProductos", null);
        Respuesta r = expo.atender("catalogo", Marshalling.marshalSolicitud(s));
        assertTrue(r.isExitosa());
        assertEquals(s.getIdSolicitud(), r.getIdSolicitud());
    }

    @Test
    @DisplayName("Un servicio que esta aplicacion no aloja devuelve SERVICIO_NO_ENCONTRADO")
    void servicioDesconocido() {
        assertEquals(EstadoRespuesta.SERVICIO_NO_ENCONTRADO, expo.atender("pedidos", json("listarProductos")).getEstado());
    }

    @Test
    @DisplayName("JSON ilegible o incompleto devuelve SOLICITUD_INVALIDA")
    void jsonInvalido() {
        assertEquals(EstadoRespuesta.SOLICITUD_INVALIDA, expo.atender("catalogo", "esto no es json").getEstado());
        assertEquals(EstadoRespuesta.SOLICITUD_INVALIDA, expo.atender("catalogo", "{\"servicio\":\"catalogo\"}").getEstado());
        assertEquals(EstadoRespuesta.SOLICITUD_INVALIDA, expo.atender("catalogo", "").getEstado());
    }

    @Test
    @DisplayName("Una solicitud para otro servicio que la URL se rechaza")
    void servicioNoCoincide() {
        String paraPedidos = Marshalling.marshalSolicitud(new Solicitud("pedidos", "x", null));
        assertEquals(EstadoRespuesta.SOLICITUD_INVALIDA, expo.atender("catalogo", paraPedidos).getEstado());
        assertEquals(0, negocio.ejecuciones.get());
    }

    @Test
    @DisplayName("Una excepcion dentro del proveedor se convierte en ERROR_PROVEEDOR (no tumba el servidor)")
    void excepcionControlada() {
        Respuesta r = expo.atender("catalogo", json("explotar"));
        assertEquals(EstadoRespuesta.ERROR_PROVEEDOR, r.getEstado());
        assertTrue(r.getMensajeError().contains("IllegalStateException"));
    }

    @Test
    @DisplayName("Misma solicitud repetida (mismo id) se ejecuta UNA sola vez")
    void duplicadosNoSeRepiten() {
        String mismoJson = json("listarProductos");
        Respuesta a = expo.atender("catalogo", mismoJson);
        Respuesta b = expo.atender("catalogo", mismoJson);
        assertTrue(a.isExitosa());
        assertTrue(b.isExitosa());
        assertEquals(1, negocio.ejecuciones.get());
    }

    @Test
    @DisplayName("Las respuestas de error NO se recuerdan: un reintento vuelve a ejecutar")
    void erroresSeReintentan() {
        String mismoJson = json("explotar");
        expo.atender("catalogo", mismoJson);
        expo.atender("catalogo", mismoJson);
        assertEquals(2, negocio.ejecuciones.get());
    }

    @Test
    @DisplayName("Autorizacion: con token configurado solo pasa el correcto; sin token configurado pasa todo")
    void autorizacion() {
        assertTrue(expo.autorizado("secreto"));
        assertFalse(expo.autorizado("otro"));
        assertFalse(expo.autorizado(null));
        assertTrue(new ExposicionProveedores(List.of(negocio), "").autorizado(null));
    }

    @Test
    @DisplayName("Health: lista los servicios alojados")
    void salud() {
        assertEquals("UP", expo.salud().get("status"));
        assertTrue(expo.salud().get("servicios").toString().contains("catalogo"));
    }
}
