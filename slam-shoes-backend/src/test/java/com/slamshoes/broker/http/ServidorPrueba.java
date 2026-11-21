package com.slamshoes.broker.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Servidor HTTP mínimo (el {@code com.sun.net.httpserver} que trae el JDK) para probar el transporte con peticiones
 * HTTP REALES, sin levantar Spring ni la base de datos. Escucha en un puerto libre de 127.0.0.1.
 *
 * Incluye dos "montajes" que reproducen, en pocas líneas, las rutas de los controladores REST
 * ({@code BrokerHttpController} y {@code ProveedorHttpController}) apoyándose en las MISMAS clases que usan
 * los controladores ({@link NucleoBroker}, {@link ExposicionProveedores}).
 */
final class ServidorPrueba implements AutoCloseable {

    private final HttpServer servidor;

    private ServidorPrueba(HttpServer servidor) {
        this.servidor = servidor;
    }

    static ServidorPrueba iniciar() throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        s.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        ServidorPrueba p = new ServidorPrueba(s);
        s.start();
        return p;
    }

    /** URL base, por ejemplo http://127.0.0.1:53211 */
    String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    ServidorPrueba ruta(String ruta, HttpHandler manejador) {
        servidor.createContext(ruta, manejador);
        return this;
    }

    @Override
    public void close() {
        servidor.stop(0);
        ((java.util.concurrent.ExecutorService) servidor.getExecutor()).shutdownNow();
    }

    // ------------------------------------------------------------------ utilidades

    static String leerCuerpo(HttpExchange x) throws IOException {
        return new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static void responder(HttpExchange x, int codigo, String tipo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", tipo);
        x.sendResponseHeaders(codigo, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            x.getResponseBody().write(bytes);
        }
        x.close();
    }

    static void json(HttpExchange x, int codigo, String cuerpo) throws IOException {
        responder(x, codigo, "application/json", cuerpo);
    }

    // ------------------------------------------------------------------ montajes de los controladores

    /** Reproduce BrokerHttpController sobre un NucleoBroker. */
    static void montarBroker(ServidorPrueba srv, NucleoBroker nucleo) {
        srv.ruta(RutasBroker.SALUD, x -> json(x, 200, Json.escribir(nucleo.salud())));
        srv.ruta(RutasBroker.ESTADO, x -> json(x, 200, Json.escribir(nucleo.estado())));
        srv.ruta(RutasBroker.REGISTRAR, x -> {
            NucleoBroker.Acuse a = nucleo.registrar(x.getRequestHeaders().getFirst(RutasBroker.CABECERA_TOKEN),
                    Marshalling.leerMensaje(leerCuerpo(x)), x.getRemoteAddress().getAddress().getHostAddress());
            json(x, a.codigo(), Json.escribir(a.aMapa()));
        });
        srv.ruta(RutasBroker.BAJA, x -> {
            NucleoBroker.Acuse a = nucleo.darDeBaja(x.getRequestHeaders().getFirst(RutasBroker.CABECERA_TOKEN),
                    Marshalling.leerMensaje(leerCuerpo(x)), "prueba");
            json(x, a.codigo(), Json.escribir(a.aMapa()));
        });
        srv.ruta(RutasBroker.SOLICITAR, x -> {
            Respuesta r;
            try {
                r = nucleo.solicitar(Marshalling.leerMensaje(leerCuerpo(x)), "prueba");
            } catch (IllegalArgumentException e) {
                r = Respuesta.error("sin-id", EstadoRespuesta.SOLICITUD_INVALIDA, e.getMessage());
            }
            json(x, 200, Marshalling.marshalRespuesta(r));
        });
        srv.ruta(RutasBroker.PROXY + "/", x -> {
            String uri = x.getRequestURI().getRawPath();
            String sinPrefijo = uri.substring((RutasBroker.PROXY + "/").length());
            int corte = sinPrefijo.indexOf('/');
            String servicio = corte < 0 ? sinPrefijo : sinPrefijo.substring(0, corte);
            String resto = corte < 0 ? "" : sinPrefijo.substring(corte);
            String cuerpo = x.getRequestMethod().equals("GET") ? null : leerCuerpo(x);
            MensajeHttp m = nucleo.getProxy().reenviar(servicio, x.getRequestMethod(), resto,
                    x.getRequestURI().getRawQuery(), cuerpo, x.getRequestHeaders().getFirst("Content-Type"),
                    x.getRequestHeaders().getFirst("Accept"));
            responder(x, m.codigo(), m.tipoContenido() == null ? "application/json" : m.tipoContenido(), m.cuerpo());
        });
    }

    /** Reproduce ProveedorHttpController sobre una ExposicionProveedores. */
    static void montarProveedores(ServidorPrueba srv, ExposicionProveedores exposicion) {
        srv.ruta(RutasBroker.PROVEEDOR + "/", x -> {
            if (!exposicion.autorizado(x.getRequestHeaders().getFirst(RutasBroker.CABECERA_TOKEN))) {
                json(x, 401, Marshalling.marshalRespuesta(
                        Respuesta.error("sin-id", EstadoRespuesta.NO_AUTORIZADO, "token")));
                return;
            }
            String servicio = x.getRequestURI().getPath().substring((RutasBroker.PROVEEDOR + "/").length());
            json(x, 200, Marshalling.marshalRespuesta(exposicion.atender(servicio, leerCuerpo(x))));
        });
    }

    /** Mapa pequeño para armar JSON en las pruebas. */
    static Map<String, Object> mapa(Object... paresClaveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            m.put(String.valueOf(paresClaveValor[i]), paresClaveValor[i + 1]);
        }
        return m;
    }
}
