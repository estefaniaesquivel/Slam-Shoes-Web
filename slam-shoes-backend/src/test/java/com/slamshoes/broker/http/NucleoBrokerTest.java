package com.slamshoes.broker.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;

/** Pruebas del registro dinámico, el health check y el enrutamiento de {@link NucleoBroker} (sin red). */
class NucleoBrokerTest {

    private NucleoBroker broker;

    @BeforeEach
    void preparar() {
        broker = new NucleoBroker(null, 500, 500, 0, 3, null);
    }

    private Map<String, Object> alta(String servicio, String url) {
        return ServidorPrueba.mapa("servicio", servicio, "url", url, "operaciones", List.of("listarProductos"));
    }

    @Test
    @DisplayName("Registro dinamico: un proveedor nuevo queda en el directorio (200)")
    void registraProveedorNuevo() {
        NucleoBroker.Acuse a = broker.registrar(null, alta("catalogo", "http://localhost:9001"), "test");
        assertEquals(200, a.codigo());
        assertTrue(a.ok());
        assertTrue(broker.getRegistro().existe("catalogo"));
    }

    @Test
    @DisplayName("Latido: el mismo proveedor avisando de nuevo no duplica ni falla")
    void latidoDelMismoProveedor() {
        broker.registrar(null, alta("catalogo", "http://localhost:9001"), "test");
        NucleoBroker.Acuse a = broker.registrar(null, alta("catalogo", "http://localhost:9001"), "test");
        assertEquals(200, a.codigo());
        assertEquals(1, broker.getRegistro().listarNombres().size());
    }

    @Test
    @DisplayName("Conflicto: otra URL para el mismo servicio se rechaza (409) salvo reemplazar=true")
    void conflictoYReemplazo() {
        broker.registrar(null, alta("catalogo", "http://localhost:9001"), "test");
        assertEquals(409, broker.registrar(null, alta("catalogo", "http://localhost:9002"), "test").codigo());

        Map<String, Object> reemplazo = alta("catalogo", "http://localhost:9002");
        reemplazo.put("reemplazar", true);
        assertEquals(200, broker.registrar(null, reemplazo, "test").codigo());
        ProveedorHttp actual = (ProveedorHttp) broker.getRegistro().buscar("catalogo").get();
        assertEquals("http://localhost:9002", actual.destino());
    }

    @Test
    @DisplayName("Un registro con datos invalidos responde 400 y no registra nada")
    void registroInvalido() {
        NucleoBroker.Acuse a = broker.registrar(null, ServidorPrueba.mapa("servicio", "x"), "test");
        assertEquals(400, a.codigo());
        assertFalse(a.ok());
        assertTrue(broker.getRegistro().listarNombres().isEmpty());
    }

    @Test
    @DisplayName("Con BROKER_TOKEN configurado, registrar sin token o con token incorrecto responde 401")
    void tokenObligatorio() {
        NucleoBroker conToken = new NucleoBroker(null, 500, 500, 0, 3, "secreto");
        assertEquals(401, conToken.registrar(null, alta("catalogo", "http://localhost:9001"), "t").codigo());
        assertEquals(401, conToken.registrar("otro", alta("catalogo", "http://localhost:9001"), "t").codigo());
        assertEquals(200, conToken.registrar("secreto", alta("catalogo", "http://localhost:9001"), "t").codigo());
        assertEquals(401, conToken.darDeBaja("otro", ServidorPrueba.mapa("servicio", "catalogo", "url", "http://localhost:9001"), "t").codigo());
    }

    @Test
    @DisplayName("Baja: solo la acepta quien esta registrado con esa URL")
    void baja() {
        broker.registrar(null, alta("catalogo", "http://localhost:9001"), "test");
        assertEquals(404, broker.darDeBaja(null, ServidorPrueba.mapa("servicio", "catalogo", "url", "http://localhost:9999"), "t").codigo());
        assertEquals(400, broker.darDeBaja(null, ServidorPrueba.mapa("servicio", "catalogo"), "t").codigo());
        assertEquals(200, broker.darDeBaja(null, ServidorPrueba.mapa("servicio", "catalogo", "url", "http://localhost:9001/"), "t").codigo());
        assertFalse(broker.getRegistro().existe("catalogo"));
    }

    @Test
    @DisplayName("Health check: responde UP sin contactar a ningun proveedor")
    void salud() {
        Map<String, Object> antes = broker.salud();
        assertEquals("UP", antes.get("status"));
        assertEquals(0, antes.get("serviciosRegistrados"));
        broker.registrar(null, alta("catalogo", "http://localhost:9001"), "test");
        assertEquals(1, broker.salud().get("serviciosRegistrados"));
    }

    @Test
    @DisplayName("Solicitar un servicio que nadie registro devuelve SERVICIO_NO_ENCONTRADO")
    void servicioNoEncontrado() {
        Respuesta r = broker.solicitar(ServidorPrueba.mapa("idSolicitud", "abc12345", "servicio", "fantasma", "operacion", "x"), "test");
        assertEquals(EstadoRespuesta.SERVICIO_NO_ENCONTRADO, r.getEstado());
    }

    @Test
    @DisplayName("Una solicitud mal formada devuelve SOLICITUD_INVALIDA (nunca una excepcion)")
    void solicitudMalFormada() {
        Respuesta r = broker.solicitar(ServidorPrueba.mapa("servicio", "catalogo"), "test");
        assertEquals(EstadoRespuesta.SOLICITUD_INVALIDA, r.getEstado());
    }

    @Test
    @DisplayName("El estado lista los proveedores con su URL y cuenta las solicitudes")
    void estado() {
        broker.registrar(null, alta("catalogo", "http://localhost:9001"), "test");
        broker.solicitar(ServidorPrueba.mapa("idSolicitud", "abc12345", "servicio", "fantasma", "operacion", "x"), "test");
        Map<String, Object> estado = broker.estado();
        List<?> servicios = (List<?>) estado.get("servicios");
        Map<?, ?> uno = (Map<?, ?>) servicios.get(0);
        assertEquals("catalogo", uno.get("servicio"));
        assertEquals("http://localhost:9001", uno.get("url"));
        assertEquals(1L, ((Map<?, ?>) estado.get("metricas")).get("solicitudesTotales"));
        assertEquals("HTTP", estado.get("transporte"));
    }
}
