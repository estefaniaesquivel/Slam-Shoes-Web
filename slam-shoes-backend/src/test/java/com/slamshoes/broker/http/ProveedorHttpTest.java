package com.slamshoes.broker.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.ProveedorNoDisponibleException;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/** Pruebas de {@link ProveedorHttp}: el Broker habla con un proveedor REAL por HTTP (servidor de prueba). */
class ProveedorHttpTest {

    private static ProveedorHttp proveedorEn(ServidorPrueba srv, int msLectura, int reintentos, String token) {
        RegistroProveedor datos = RegistroProveedor.desdeMapa(
                ServidorPrueba.mapa("servicio", "catalogo", "url", srv.url()));
        return new ProveedorHttp(datos, 500, msLectura, reintentos, token);
    }

    private static Solicitud listar() {
        return new Solicitud("catalogo", "listarProductos", null);
    }

    @Test
    @DisplayName("Ejecutar: la Solicitud viaja por HTTP y vuelve la Respuesta con el mismo id")
    void ejecutaPorHttp() throws Exception {
        ProveedorFalso negocio = new ProveedorFalso("catalogo");
        try (ServidorPrueba srv = ServidorPrueba.iniciar()) {
            ServidorPrueba.montarProveedores(srv, new ExposicionProveedores(List.of(negocio), null));
            Solicitud s = listar();
            Respuesta r = proveedorEn(srv, 2000, 0, null).ejecutar(s);
            assertTrue(r.isExitosa());
            assertEquals(s.getIdSolicitud(), r.getIdSolicitud());
            assertEquals(1, ((List<?>) r.getResultado()).size());
            assertEquals(1, negocio.ejecuciones.get());
        }
    }

    @Test
    @DisplayName("Reintenta con 503 (servicio despertando) y termina bien")
    void reintentaConTransitorios() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        ProveedorFalso negocio = new ProveedorFalso("catalogo");
        ExposicionProveedores expo = new ExposicionProveedores(List.of(negocio), null);
        try (ServidorPrueba srv = ServidorPrueba.iniciar()) {
            srv.ruta(RutasBroker.PROVEEDOR + "/", x -> {
                String cuerpo = ServidorPrueba.leerCuerpo(x);
                if (llamadas.incrementAndGet() <= 2) {
                    ServidorPrueba.json(x, 503, "{\"error\":\"despertando\"}");
                } else {
                    ServidorPrueba.json(x, 200, Marshalling.marshalRespuesta(expo.atender("catalogo", cuerpo)));
                }
            });
            Respuesta r = proveedorEn(srv, 2000, 2, null).ejecutar(listar());
            assertTrue(r.isExitosa());
            assertEquals(3, llamadas.get());
        }
    }

    @Test
    @DisplayName("Si siempre responde 503 lanza ProveedorNoDisponibleException (no es tiempo agotado)")
    void agotaReintentos() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        try (ServidorPrueba srv = ServidorPrueba.iniciar()) {
            srv.ruta(RutasBroker.PROVEEDOR + "/", x -> {
                ServidorPrueba.leerCuerpo(x);
                llamadas.incrementAndGet();
                ServidorPrueba.json(x, 503, "{}");
            });
            ProveedorHttp p = proveedorEn(srv, 2000, 1, null);
            ProveedorNoDisponibleException e = assertThrows(ProveedorNoDisponibleException.class, () -> p.ejecutar(listar()));
            assertFalse(e.isTiempoAgotado());
            assertEquals(2, llamadas.get());
            assertEquals(1, p.getFallosConsecutivos());
        }
    }

    @Test
    @DisplayName("Si el proveedor tarda mas que el timeout lanza la excepcion marcada como tiempo agotado")
    void tiempoAgotado() throws Exception {
        try (ServidorPrueba srv = ServidorPrueba.iniciar()) {
            srv.ruta(RutasBroker.PROVEEDOR + "/", x -> {
                ServidorPrueba.leerCuerpo(x);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ServidorPrueba.json(x, 200, "{}");
            });
            ProveedorHttp p = proveedorEn(srv, 200, 0, null);
            ProveedorNoDisponibleException e = assertThrows(ProveedorNoDisponibleException.class, () -> p.ejecutar(listar()));
            assertTrue(e.isTiempoAgotado());
        }
    }

    @Test
    @DisplayName("Si el proveedor esta apagado (conexion rechazada) lanza ProveedorNoDisponibleException")
    void proveedorApagado() throws Exception {
        ProveedorHttp p;
        try (ServidorPrueba srv = ServidorPrueba.iniciar()) {
            p = proveedorEn(srv, 500, 0, null);
        } // el servidor se cierra al salir del bloque
        ProveedorNoDisponibleException e = assertThrows(ProveedorNoDisponibleException.class, () -> p.ejecutar(listar()));
        assertFalse(e.isTiempoAgotado());
    }

    @Test
    @DisplayName("HTTP 404 o cuerpo ilegible no se reintentan: IllegalStateException (el Broker lo vuelve ERROR_PROVEEDOR)")
    void erroresDefinitivos() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        AtomicInteger modo = new AtomicInteger(404);
        try (ServidorPrueba srv = ServidorPrueba.iniciar()) {
            srv.ruta(RutasBroker.PROVEEDOR + "/", x -> {
                ServidorPrueba.leerCuerpo(x);
                llamadas.incrementAndGet();
                ServidorPrueba.responder(x, modo.get(), "text/plain", modo.get() == 200 ? "esto no es json" : "no existe");
            });
            ProveedorHttp p = proveedorEn(srv, 1000, 3, null);
            assertThrows(IllegalStateException.class, () -> p.ejecutar(listar()));
            assertEquals(1, llamadas.get());
            modo.set(200);
            assertThrows(IllegalStateException.class, () -> p.ejecutar(listar()));
            assertEquals(2, llamadas.get());
        }
    }

    @Test
    @DisplayName("Envia el token compartido en la cabecera X-Broker-Token")
    void enviaToken() throws Exception {
        ExposicionProveedores expo = new ExposicionProveedores(List.of(new ProveedorFalso("catalogo")), "secreto");
        try (ServidorPrueba srv = ServidorPrueba.iniciar()) {
            ServidorPrueba.montarProveedores(srv, expo);
            assertTrue(proveedorEn(srv, 2000, 0, "secreto").ejecutar(listar()).isExitosa());
            // sin token el proveedor contesta 401 y eso es un error definitivo
            assertThrows(IllegalStateException.class, () -> proveedorEn(srv, 2000, 0, null).ejecutar(listar()));
        }
    }

    @Test
    @DisplayName("Un error controlado del proveedor (PARAMETROS_INVALIDOS) llega intacto al Broker")
    void erroresDeNegocioPasanIntactos() throws Exception {
        try (ServidorPrueba srv = ServidorPrueba.iniciar()) {
            ServidorPrueba.montarProveedores(srv, new ExposicionProveedores(List.of(new ProveedorFalso("catalogo")), null));
            Respuesta r = proveedorEn(srv, 2000, 0, null).ejecutar(new Solicitud("catalogo", "fallarControlado", null));
            assertFalse(r.isExitosa());
            assertEquals(EstadoRespuesta.PARAMETROS_INVALIDOS, r.getEstado());
            assertEquals("falta algo", r.getMensajeError());
        }
    }
}
