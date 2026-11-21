package com.slamshoes.broker.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;

/**
 * CRITERIO DE ACEPTACION (equivale a la verificacion en Bruno): se levantan un Broker y un proveedor como servidores
 * HTTP reales, el proveedor se registra solo, y un cliente HTTP externo consulta al Broker y recibe 200 con el JSON.
 */
class FlujoHttpIntegracionTest {

    private static final String TOKEN = "secreto-de-prueba";

    private static void esperar(java.util.function.BooleanSupplier condicion, long ms) throws InterruptedException {
        long limite = System.currentTimeMillis() + ms;
        while (!condicion.getAsBoolean() && System.currentTimeMillis() < limite) {
            Thread.sleep(50);
        }
    }

    @Test
    @DisplayName("Registro por HTTP + GET al Broker enruta al proveedor y devuelve 200 con JSON")
    void flujoCompleto() throws Exception {
        NucleoBroker nucleo = new NucleoBroker(null, 500, 2000, 1, 3, TOKEN);
        ProveedorFalso negocio = new ProveedorFalso("catalogo");
        ExposicionProveedores expo = new ExposicionProveedores(List.of(negocio), TOKEN);

        try (ServidorPrueba broker = ServidorPrueba.iniciar(); ServidorPrueba prov = ServidorPrueba.iniciar()) {
            ServidorPrueba.montarBroker(broker, nucleo);
            ServidorPrueba.montarProveedores(prov, expo);
            // REST estandar del proveedor (lo que alcanza el proxy)
            prov.ruta("/api/catalogo/productos", x ->
                    ServidorPrueba.json(x, 200, "[{\"idProducto\":1,\"nombre\":\"Kyrie 5\"}]"));

            // 1) El proveedor se auto-registra por HTTP al arrancar
            RegistroAutomatico registro = new RegistroAutomatico(negocio, prov.url(), "/api/catalogo",
                    broker.url(), TOKEN, false, 400);
            registro.iniciar();
            try {
                esperar(registro::estaRegistrado, 5000);
                assertTrue(registro.estaRegistrado(), "el proveedor debio quedar registrado");
                assertTrue(nucleo.getRegistro().existe("catalogo"));
                assertTrue(nucleo.getBitacora().ultimas().stream().anyMatch(l -> l.contains("registrado via HTTP")),
                        "la bitacora del Broker debe decir que se registro via HTTP");

                // 2) Como Bruno: GET al Broker -> se enruta al proveedor -> 200 + JSON
                MensajeHttp m = TransporteHttp.enviar("GET", URI.create(broker.url() + "/api/gateway/catalogo/productos"),
                        null, Map.of("Accept", "application/json"), 1000, 3000);
                assertEquals(200, m.codigo());
                assertTrue(m.tipoContenido().startsWith("application/json"));
                assertTrue(m.cuerpo().contains("Kyrie 5"));

                // 3) Patron Broker completo (Solicitud/Respuesta) desde un cliente remoto
                ClienteHttp cliente = new ClienteHttp(broker.url());
                Respuesta r = cliente.procesar(new Solicitud("catalogo", "listarProductos", null));
                assertTrue(r.isExitosa());
                assertEquals(1, ((List<?>) r.getResultado()).size());
                assertEquals(1, negocio.ejecuciones.get());

                // 4) Una operacion que el proveedor no declaro la rechaza el Broker sin molestar al proveedor
                Respuesta noExiste = cliente.procesar(new Solicitud("catalogo", "borrarTodo", null));
                assertEquals(EstadoRespuesta.OPERACION_NO_SOPORTADA, noExiste.getEstado());
                assertEquals(1, negocio.ejecuciones.get());

                // 5) El estado muestra el proveedor con su URL
                Map<String, Object> estado = cliente.consultarEstado();
                assertTrue(Json.escribir(estado).contains(prov.url()));

                // 6) Si el Broker "se reinicia" (pierde su directorio en memoria) el latido lo vuelve a registrar
                nucleo.getRegistro().eliminar("catalogo", nucleo.getRegistro().buscar("catalogo").get());
                assertFalse(nucleo.getRegistro().existe("catalogo"));
                esperar(() -> nucleo.getRegistro().existe("catalogo"), 5000);
                assertTrue(nucleo.getRegistro().existe("catalogo"), "el latido debio re-registrar al proveedor");
            } finally {
                registro.detener();
            }
            // 7) Al detenerse, el proveedor se da de baja
            assertFalse(nucleo.getRegistro().existe("catalogo"));
        }
    }

    @Test
    @DisplayName("Un proveedor con token incorrecto no logra registrarse")
    void tokenIncorrecto() throws Exception {
        NucleoBroker nucleo = new NucleoBroker(null, 500, 1000, 0, 3, TOKEN);
        try (ServidorPrueba broker = ServidorPrueba.iniciar()) {
            ServidorPrueba.montarBroker(broker, nucleo);
            RegistroAutomatico intruso = new RegistroAutomatico(new ProveedorFalso("catalogo"), "http://localhost:1",
                    "", broker.url(), "otro-token", false, 400);
            assertFalse(intruso.registrarUnaVez(false));
            assertFalse(nucleo.getRegistro().existe("catalogo"));
        }
    }

    @Test
    @DisplayName("Si el Broker esta apagado, el cliente recibe BROKER_NO_DISPONIBLE (sin excepciones)")
    void brokerApagado() throws Exception {
        String url;
        try (ServidorPrueba broker = ServidorPrueba.iniciar()) {
            url = broker.url();
        }
        Respuesta r = new ClienteHttp(url, 300, 500, 0).procesar(new Solicitud("catalogo", "listarProductos", null));
        assertEquals(EstadoRespuesta.BROKER_NO_DISPONIBLE, r.getEstado());
    }

    @Test
    @DisplayName("Si el proveedor no esta, el Broker devuelve PROVEEDOR_NO_DISPONIBLE y lo retira tras 3 fallos")
    void proveedorCaidoSeRetira() throws Exception {
        NucleoBroker nucleo = new NucleoBroker(null, 300, 500, 0, 3, null);
        String urlMuerta;
        try (ServidorPrueba prov = ServidorPrueba.iniciar()) {
            urlMuerta = prov.url();
        }
        nucleo.registrar(null, ServidorPrueba.mapa("servicio", "catalogo", "url", urlMuerta,
                "operaciones", List.of("listarProductos")), "test");
        for (int i = 1; i <= 3; i++) {
            Respuesta r = nucleo.solicitar(new Solicitud("catalogo", "listarProductos", null), "test");
            assertEquals(EstadoRespuesta.PROVEEDOR_NO_DISPONIBLE, r.getEstado());
        }
        assertFalse(nucleo.getRegistro().existe("catalogo"));
        assertEquals(EstadoRespuesta.SERVICIO_NO_ENCONTRADO,
                nucleo.solicitar(new Solicitud("catalogo", "listarProductos", null), "test").getEstado());
    }
}
