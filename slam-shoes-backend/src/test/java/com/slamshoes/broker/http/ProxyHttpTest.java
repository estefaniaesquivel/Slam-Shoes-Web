package com.slamshoes.broker.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas del reenvío transparente {@code /api/gateway/{servicio}/**} ({@link ProxyHttp}). */
class ProxyHttpTest {

    private static NucleoBroker brokerCon(String servicio, String url, String rutaBase, String token) {
        NucleoBroker b = new NucleoBroker(null, 300, 400, 0, 3, token);
        NucleoBroker.Acuse a = b.registrar(token, ServidorPrueba.mapa("servicio", servicio, "url", url,
                "rutaBase", rutaBase), "test");
        assertEquals(200, a.codigo());
        return b;
    }

    @Test
    @DisplayName("GET se reenvia al proveedor con su ruta base y parametros, y vuelve 200 con el JSON")
    void getReenviado() throws Exception {
        AtomicReference<String> visto = new AtomicReference<>();
        try (ServidorPrueba prov = ServidorPrueba.iniciar()) {
            prov.ruta("/api/productos", x -> {
                visto.set(x.getRequestMethod() + " " + x.getRequestURI());
                ServidorPrueba.json(x, 200, "[{\"idProducto\":1,\"nombre\":\"Kyrie 5\"}]");
            });
            NucleoBroker broker = brokerCon("productos", prov.url(), "/api/productos", null);
            MensajeHttp m = broker.getProxy().reenviar("productos", "GET", "", "marca=nike", null, null, "application/json");
            assertEquals(200, m.codigo());
            assertTrue(m.cuerpo().contains("Kyrie 5"));
            assertEquals("GET /api/productos?marca=nike", visto.get());
        }
    }

    @Test
    @DisplayName("POST reenvia cuerpo y Content-Type, y la ruta que sigue al servicio")
    void postReenviado() throws Exception {
        AtomicReference<String> visto = new AtomicReference<>();
        try (ServidorPrueba prov = ServidorPrueba.iniciar()) {
            prov.ruta("/api/pedidos/", x -> {
                visto.set(x.getRequestURI().getPath() + "|" + x.getRequestHeaders().getFirst("Content-Type")
                        + "|" + ServidorPrueba.leerCuerpo(x));
                ServidorPrueba.json(x, 201, "{\"creado\":true}");
            });
            NucleoBroker broker = brokerCon("pedidos", prov.url(), "/api/pedidos", null);
            MensajeHttp m = broker.getProxy().reenviar("pedidos", "POST", "/nuevo", null, "{\"a\":1}", "application/json", null);
            assertEquals(201, m.codigo());
            assertEquals("/api/pedidos/nuevo|application/json|{\"a\":1}", visto.get());
        }
    }

    @Test
    @DisplayName("Un codigo del proveedor (por ejemplo 404) se entrega tal cual al cliente")
    void codigoDelProveedorSePreserva() throws Exception {
        try (ServidorPrueba prov = ServidorPrueba.iniciar()) {
            prov.ruta("/api/productos", x -> ServidorPrueba.json(x, 404, "{\"error\":\"no hay\"}"));
            NucleoBroker broker = brokerCon("productos", prov.url(), "/api/productos", null);
            assertEquals(404, broker.getProxy().reenviar("productos", "GET", "", null, null, null, null).codigo());
        }
    }

    @Test
    @DisplayName("Servicio no registrado: 404 con estado SERVICIO_NO_ENCONTRADO")
    void servicioDesconocido() {
        NucleoBroker broker = new NucleoBroker(null, 300, 400, 0, 3, null);
        MensajeHttp m = broker.getProxy().reenviar("fantasma", "GET", "/x", null, null, null, null);
        assertEquals(404, m.codigo());
        assertTrue(m.cuerpo().contains("SERVICIO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Seguridad: rutas con .. o separadores codificados se rechazan (400) y el metodo TRACE tambien (405)")
    void seguridad() throws Exception {
        try (ServidorPrueba prov = ServidorPrueba.iniciar()) {
            prov.ruta("/", x -> ServidorPrueba.json(x, 200, "{}"));
            NucleoBroker broker = brokerCon("productos", prov.url(), "/api/productos", null);
            for (String ruta : List.of("/../admin", "/%2e%2e/admin", "/a%2fb", "/a%5cb", "/a b", "//x/../y")) {
                assertEquals(400, broker.getProxy().reenviar("productos", "GET", ruta, null, null, null, null).codigo(), ruta);
            }
            assertEquals(405, broker.getProxy().reenviar("productos", "TRACE", "", null, null, null, null).codigo());
        }
    }

    @Test
    @DisplayName("Proveedor apagado: 502; proveedor lento: 504")
    void fallosDeRed() throws Exception {
        NucleoBroker apagado;
        try (ServidorPrueba prov = ServidorPrueba.iniciar()) {
            apagado = brokerCon("productos", prov.url(), "/api/productos", null);
        }
        assertEquals(502, apagado.getProxy().reenviar("productos", "GET", "", null, null, null, null).codigo());

        try (ServidorPrueba lento = ServidorPrueba.iniciar()) {
            lento.ruta("/", x -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ServidorPrueba.json(x, 200, "{}");
            });
            NucleoBroker broker = brokerCon("productos", lento.url(), "/api/productos", null);
            assertEquals(504, broker.getProxy().reenviar("productos", "GET", "", null, null, null, null).codigo());
        }
    }

    @Test
    @DisplayName("Reenvia el token compartido al proveedor")
    void reenviaToken() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        try (ServidorPrueba prov = ServidorPrueba.iniciar()) {
            prov.ruta("/api/productos", x -> {
                token.set(x.getRequestHeaders().getFirst(RutasBroker.CABECERA_TOKEN));
                ServidorPrueba.json(x, 200, "[]");
            });
            NucleoBroker broker = brokerCon("productos", prov.url(), "/api/productos", "secreto");
            broker.getProxy().reenviar("productos", "GET", "", null, null, null, null);
            assertEquals("secreto", token.get());
        }
    }
}
