package com.slamshoes.nivel2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.broker.cliente.ClienteBroker;
import com.slamshoes.broker.red.BrokerServidor;
import com.slamshoes.broker.red.Canal;
import com.slamshoes.broker.red.ClienteSocket;
import com.slamshoes.broker.red.Json;
import com.slamshoes.broker.red.Marshalling;
import com.slamshoes.broker.red.ProveedorRemoto;
import com.slamshoes.broker.red.ProveedorServidor;
import com.slamshoes.providers.ServicioPedidos;
import com.slamshoes.providers.ServicioPedidosEnMemoria;
import com.slamshoes.providers.ServicioProductos;
import com.slamshoes.providers.ServicioPruebas;
import com.slamshoes.providers.ServicioUsuarios;

/**
 * Pruebas AUTOMATICAS del Nivel 2: levanta el Broker, los proveedores y el cliente en esta misma
 * JVM (cada uno con su propio socket TCP real, en puertos libres) y verifica los 8 puntos del
 * documento y varios mas (timeouts, caidas, duplicados, concurrencia, reconexion).
 *
 * Uso:  java -cp target/nivel2 com.slamshoes.nivel2.PruebasNivel2 | tee evidencia-nivel2.txt
 * Termina con codigo 0 si todo paso, 1 si algo fallo.
 */
public final class PruebasNivel2 {

    private static int pasaron = 0;
    private static int fallaron = 0;
    private static final List<String> tabla = new ArrayList<>();

    private PruebasNivel2() {
    }

    public static void main(String[] args) throws Exception {
        Path bitacora = Paths.get("bitacora-pruebas-nivel2.log");
        Files.deleteIfExists(bitacora);

        // Timeouts cortos para que la prueba sea rapida: conectar 500 ms, responder 800 ms, 1 reintento
        BrokerServidor broker = new BrokerServidor(0, bitacora, 500, 800, 1);
        broker.iniciar();
        int pb = broker.getPuerto();

        ClienteSocket socket = new ClienteSocket("localhost", pb, 1000, 8000, 1);
        ClienteBroker cliente = new ClienteBroker(socket);

        Consola.titulo("PREPARACION: se inician 4 proveedores (cada uno con su socket) y se registran solos");
        ProveedorServidor productos = iniciar(new ServicioProductos(), pb, false);
        ProveedorServidor usuarios = iniciar(new ServicioUsuarios(), pb, false);
        ProveedorServidor pedidos = iniciar(new ServicioPedidos(), pb, false);
        ProveedorServidor pruebas = iniciar(new ServicioPruebas(), pb, false);
        esperar(() -> broker.getRegistro().listarNombres().size() == 4, 5000);

        // ---------------------------------------------------------------- 1
        Consola.titulo("PRUEBA 1. Registro correcto de proveedores");
        Map<String, Object> estado = socket.consultarEstado();
        List<?> servicios = (List<?>) estado.get("servicios");
        for (Object s : servicios) {
            Consola.cliente("  registrado: " + s);
        }
        comprobar("1. Registro correcto de proveedores",
                broker.getRegistro().listarNombres().equals(new java.util.TreeSet<>(List.of("pedidos", "productos", "pruebas", "usuarios"))),
                "servicios=" + broker.getRegistro().listarNombres());

        // ---------------------------------------------------------------- 2
        Consola.titulo("PRUEBA 2. Localizacion de un servicio");
        Object destino = broker.getRegistro().buscar("productos").orElse(null);
        Consola.cliente("El registro localiza 'productos' en: " + destino);
        comprobar("2. Localizacion de un servicio",
                destino instanceof ProveedorRemoto r && r.getPuerto() == productos.getPuerto(),
                "productos -> puerto " + productos.getPuerto());

        // ---------------------------------------------------------------- 3 y 4
        Consola.titulo("PRUEBAS 3 y 4. Ejecucion de una operacion y respuesta exitosa");
        Solicitud pedido1 = new Solicitud("productos", "obtenerDetalle", Map.of("idProducto", 1));
        Respuesta exito = cliente.enviar("productos", "obtenerDetalle", Map.of("idProducto", 1));
        comprobar("3. Ejecucion de una operacion (la atendio el proveedor remoto)",
                exito.getResultado() instanceof Map<?, ?> m && "ServicioProductos".equals(m.get("atendidoPor")),
                "atendidoPor=ServicioProductos");
        comprobar("4. Respuesta exitosa (estado EXITO con resultado)",
                exito.isExitosa() && exito.getResultado() instanceof Map<?, ?> m2 && "Kyrie 5 SpongeBob".equals(m2.get("nombre")),
                "estado=" + exito.getEstado());
        Respuesta conMismoId = socket.procesar(pedido1);
        comprobar("4b. La respuesta conserva el idSolicitud de la solicitud",
                conMismoId.getIdSolicitud().equals(pedido1.getIdSolicitud()), pedido1.getIdCorto());

        // ---------------------------------------------------------------- 5
        Consola.titulo("PRUEBA 5. Servicio desconocido");
        Respuesta desconocido = cliente.enviar("envios", "calcularCosto", Map.of("codigoPostal", "97000"));
        comprobar("5. Servicio desconocido -> SERVICIO_NO_ENCONTRADO",
                desconocido.getEstado() == EstadoRespuesta.SERVICIO_NO_ENCONTRADO, String.valueOf(desconocido.getEstado()));

        // ---------------------------------------------------------------- 6
        Consola.titulo("PRUEBA 6. Parametro invalido");
        Respuesta invalido = cliente.enviar("productos", "obtenerDetalle", Map.of("idProducto", "abc"));
        Respuesta faltante = cliente.enviar("productos", "obtenerDetalle", Map.of());
        Respuesta cantidad = cliente.enviar("pedidos", "crearPedido", Map.of("idUsuario", 1, "direccionEnvio", "Calle 60 #123",
                "items", List.of(Map.of("idVariante", 101, "cantidad", 0))));
        comprobar("6. Parametro invalido ('abc') -> PARAMETROS_INVALIDOS", invalido.getEstado() == EstadoRespuesta.PARAMETROS_INVALIDOS,
                invalido.getMensajeError());
        comprobar("6b. Parametro faltante -> PARAMETROS_INVALIDOS", faltante.getEstado() == EstadoRespuesta.PARAMETROS_INVALIDOS,
                faltante.getMensajeError());
        comprobar("6c. Cantidad 0 en un pedido -> PARAMETROS_INVALIDOS", cantidad.getEstado() == EstadoRespuesta.PARAMETROS_INVALIDOS,
                cantidad.getMensajeError());

        // ---------------------------------------------------------------- 7
        Consola.titulo("PRUEBA 7. Excepcion producida por un proveedor");
        Respuesta excepcion = cliente.enviar("pruebas", "fallar", Map.of());
        comprobar("7. Excepcion en el proveedor -> ERROR_PROVEEDOR (con mensaje)",
                excepcion.getEstado() == EstadoRespuesta.ERROR_PROVEEDOR
                        && String.valueOf(excepcion.getMensajeError()).contains("fallo simulado"),
                excepcion.getMensajeError());
        Respuesta despuesDelFallo = cliente.enviar("productos", "consultarStock", Map.of("idVariante", 101));
        comprobar("7b. El Broker y los demas proveedores siguen funcionando", despuesDelFallo.isExitosa(), "estado=" + despuesDelFallo.getEstado());

        // ---------------------------------------------------------------- 8
        Consola.titulo("PRUEBA 8. Cambio de proveedor sin modificar el cliente");
        Respuesta antes = cliente.enviar("pedidos", "consultarHistorial", Map.of("idUsuario", 1));
        String quienAntes = String.valueOf(((Map<?, ?>) antes.getResultado()).get("atendidoPor"));
        ProveedorServidor alterno = iniciar(new ServicioPedidosEnMemoria(), pb, true);
        esperar(() -> broker.getRegistro().buscar("pedidos").map(p -> p instanceof ProveedorRemoto r && r.getPuerto() == alterno.getPuerto()).orElse(false), 5000);
        Respuesta despues = cliente.enviar("pedidos", "consultarHistorial", Map.of("idUsuario", 1));
        String quienDespues = String.valueOf(((Map<?, ?>) despues.getResultado()).get("atendidoPor"));
        comprobar("8. Cambio de proveedor sin modificar el cliente (" + quienAntes + " -> " + quienDespues + ")",
                antes.isExitosa() && despues.isExitosa() && !quienAntes.equals(quienDespues), "mismo cliente, misma solicitud");
        // El proveedor original sigue vivo pero ya no recibe trafico; se detiene para dejar limpio el entorno
        pedidos.detenerSinAvisar();
        pedidos = alterno;

        // ---------------------------------------------------------------- extras
        Consola.titulo("EXTRA 9. Operacion que el servicio no ofrece");
        Respuesta noSoportada = cliente.enviar("productos", "borrarCatalogo", Map.of());
        comprobar("9. Operacion no ofrecida -> OPERACION_NO_SOPORTADA", noSoportada.getEstado() == EstadoRespuesta.OPERACION_NO_SOPORTADA,
                noSoportada.getMensajeError());

        Consola.titulo("EXTRA 10. Mensajes mal formados enviados directamente al Broker");
        String basura = Canal.intercambiar("localhost", pb, "esto no es json {{{", 1000, 2000);
        Respuesta rBasura = Marshalling.unmarshalRespuesta(Marshalling.leerMensaje(basura));
        String sinCampos = Canal.intercambiar("localhost", pb, "{\"tipo\":\"SOLICITUD\",\"servicio\":\"productos\"}", 1000, 2000);
        Respuesta rSinCampos = Marshalling.unmarshalRespuesta(Marshalling.leerMensaje(sinCampos));
        comprobar("10. JSON invalido -> SOLICITUD_INVALIDA (el Broker no se cae)", rBasura.getEstado() == EstadoRespuesta.SOLICITUD_INVALIDA,
                rBasura.getMensajeError());
        comprobar("10b. Solicitud sin operacion ni id -> SOLICITUD_INVALIDA", rSinCampos.getEstado() == EstadoRespuesta.SOLICITUD_INVALIDA,
                rSinCampos.getMensajeError());

        Consola.titulo("EXTRA 11. Marshalling y unmarshalling (ida y vuelta)");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("texto", "Merida \"Yucatan\" \u00f1\u00e1 \u2713\nsegunda linea\tcon tab \\ barra");
        params.put("numero", 42);
        params.put("decimal", 3499.5);
        params.put("lista", List.of(1, "dos", true));
        params.put("nulo", null);
        Solicitud original = new Solicitud("pruebas", "eco", params);
        String json = Marshalling.marshalSolicitud(original);
        Solicitud copia = Marshalling.unmarshalSolicitud(Marshalling.leerMensaje(json));
        // Se muestra el JSON de una solicitud simple (ASCII) para que se lea bien en cualquier terminal
        Consola.cliente("Ejemplo de JSON que viaja por el socket (Solicitud -> marshal): " + Marshalling.marshalSolicitud(pedido1));
        Consola.cliente("Ejemplo de JSON de la respuesta (Respuesta -> marshal):          " + Marshalling.marshalRespuesta(exito));
        comprobar("11. Solicitud -> JSON -> Solicitud conserva id, servicio, operacion y parametros",
                copia.getIdSolicitud().equals(original.getIdSolicitud()) && copia.getServicio().equals("pruebas")
                        && copia.getOperacion().equals("eco")
                        && copia.getParametros().get("texto").equals(params.get("texto"))
                        && ((Number) copia.getParametros().get("numero")).longValue() == 42
                        && ((Number) copia.getParametros().get("decimal")).doubleValue() == 3499.5
                        && copia.getParametros().get("nulo") == null
                        && !json.contains("\n"),
                "JSON de una sola linea");
        Respuesta eco = socket.procesar(original);
        comprobar("11b. El texto con acentos, comillas y saltos de linea viaja intacto por los 3 procesos",
                eco.isExitosa() && params.get("texto").equals(((Map<?, ?>) eco.getResultado()).get("eco")), "eco identico");
        comprobar("11c. Json.leer rechaza JSON incompleto", fallaJson("{\"a\":"), "lanza JsonException");

        Consola.titulo("EXTRA 12. Timeout: el proveedor tarda mas que el limite del Broker (800 ms)");
        long t0 = System.nanoTime();
        Respuesta lenta = cliente.enviar("pruebas", "dormir", Map.of("ms", 3000));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        comprobar("12. Proveedor lento -> TIEMPO_AGOTADO (respondio en " + ms + " ms, no en 3000)",
                lenta.getEstado() == EstadoRespuesta.TIEMPO_AGOTADO && ms < 3000, lenta.getMensajeError());

        Consola.titulo("EXTRA 13. Baja ordenada de un proveedor (registro dinamico)");
        pruebas.detener();
        Respuesta trasBaja = cliente.enviar("pruebas", "eco", Map.of("texto", "hola"));
        comprobar("13. Proveedor dado de baja -> SERVICIO_NO_ENCONTRADO", trasBaja.getEstado() == EstadoRespuesta.SERVICIO_NO_ENCONTRADO,
                String.valueOf(trasBaja.getEstado()));

        Consola.titulo("EXTRA 14. Alta dinamica: un proveedor nuevo aparece sin reiniciar el Broker");
        pruebas = iniciar(new ServicioPruebas(), pb, false);
        esperar(() -> broker.getRegistro().existe("pruebas"), 5000);
        Respuesta trasAlta = cliente.enviar("pruebas", "eco", Map.of("texto", "volvi"));
        comprobar("14. Proveedor registrado en caliente -> EXITO", trasAlta.isExitosa(), String.valueOf(trasAlta.getEstado()));

        Consola.titulo("EXTRA 15. Proveedor que se CAE sin avisar (desconexion)");
        pruebas.detenerSinAvisar();
        Respuesta caido1 = cliente.enviar("pruebas", "eco", Map.of("texto", "hola"));
        comprobar("15. Proveedor caido -> PROVEEDOR_NO_DISPONIBLE (tras reintentos limitados)",
                caido1.getEstado() == EstadoRespuesta.PROVEEDOR_NO_DISPONIBLE, caido1.getMensajeError());
        cliente.enviar("pruebas", "eco", Map.of("texto", "hola"));
        cliente.enviar("pruebas", "eco", Map.of("texto", "hola"));
        Respuesta retirado = cliente.enviar("pruebas", "eco", Map.of("texto", "hola"));
        comprobar("15b. Tras 3 fallos seguidos el Broker lo retira -> SERVICIO_NO_ENCONTRADO",
                retirado.getEstado() == EstadoRespuesta.SERVICIO_NO_ENCONTRADO, String.valueOf(retirado.getEstado()));
        pruebas = iniciar(new ServicioPruebas(), pb, false);
        esperar(() -> broker.getRegistro().existe("pruebas"), 5000);
        Respuesta recuperado = cliente.enviar("pruebas", "eco", Map.of("texto", "recuperado"));
        comprobar("15c. Al reiniciarse el proveedor se registra solo y vuelve a funcionar", recuperado.isExitosa(),
                String.valueOf(recuperado.getEstado()));

        Consola.titulo("EXTRA 16. Solicitud duplicada (mismo idSolicitud)");
        Solicitud repetida = new Solicitud("pruebas", "contador", Map.of());
        Respuesta d1 = socket.procesar(repetida);
        Respuesta d2 = socket.procesar(repetida);
        comprobar("16. Misma solicitud dos veces por el Broker -> se ejecuta UNA vez",
                d1.isExitosa() && d2.isExitosa() && String.valueOf(d1.getResultado()).equals(String.valueOf(d2.getResultado())),
                d1.getResultado() + " / " + d2.getResultado());
        Solicitud directa = new Solicitud("pruebas", "contador", Map.of());
        String jsonDirecto = Marshalling.marshalSolicitud(directa);
        String x1 = Canal.intercambiar("localhost", pruebas.getPuerto(), jsonDirecto, 1000, 2000);
        String x2 = Canal.intercambiar("localhost", pruebas.getPuerto(), jsonDirecto, 1000, 2000);
        comprobar("16b. El proveedor tambien descarta duplicados por si solo (reintentos del Broker)",
                Marshalling.unmarshalRespuesta(Marshalling.leerMensaje(x1)).getResultado().toString()
                        .equals(Marshalling.unmarshalRespuesta(Marshalling.leerMensaje(x2)).getResultado().toString()),
                "mismo contador");

        Consola.titulo("EXTRA 17. Atencion concurrente: 8 clientes a la vez, cada uno pide una operacion de 400 ms");
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        List<Future<Respuesta>> futuros = new ArrayList<>();
        long inicio = System.nanoTime();
        for (int i = 0; i < 8; i++) {
            futuros.add(hilos.submit(() -> socket.procesar(new Solicitud("pruebas", "dormir", Map.of("ms", 400)))));
        }
        int exitosas = 0;
        for (Future<Respuesta> f : futuros) {
            if (f.get(10, TimeUnit.SECONDS).isExitosa()) {
                exitosas++;
            }
        }
        long total = (System.nanoTime() - inicio) / 1_000_000;
        hilos.shutdownNow();
        comprobar("17. 8 solicitudes simultaneas: " + exitosas + " exitosas en " + total + " ms (en serie serian 3200 ms)",
                exitosas == 8 && total < 2000, "atencion con hilos");

        Consola.titulo("EXTRA 18. El Broker se cae y vuelve: los proveedores se re-registran solos");
        broker.detener();
        Respuesta sinBroker = cliente.enviar("productos", "consultarStock", Map.of("idVariante", 101));
        comprobar("18. Broker apagado -> BROKER_NO_DISPONIBLE (el cliente no se cuelga)",
                sinBroker.getEstado() == EstadoRespuesta.BROKER_NO_DISPONIBLE, sinBroker.getMensajeError());
        BrokerServidor brokerNuevo = new BrokerServidor(pb, bitacora, 500, 800, 1);
        brokerNuevo.iniciar();
        esperar(() -> brokerNuevo.getRegistro().listarNombres().size() >= 4, 6000);
        Respuesta recuperadoBroker = cliente.enviar("productos", "consultarStock", Map.of("idVariante", 101));
        comprobar("18b. Broker reiniciado: servicios restaurados " + brokerNuevo.getRegistro().listarNombres() + " sin reiniciar proveedores",
                recuperadoBroker.isExitosa() && brokerNuevo.getRegistro().listarNombres().size() >= 4,
                String.valueOf(recuperadoBroker.getEstado()));

        Consola.titulo("EXTRA 19. Bitacora de solicitudes y monitoreo");
        String contenido = Files.readString(bitacora);
        comprobar("19. La bitacora (archivo) registra solicitudes con servicio, operacion y estado",
                contenido.contains("productos.obtenerDetalle") && contenido.contains("SERVICIO_NO_ENCONTRADO")
                        && contenido.contains("TIEMPO_AGOTADO"), bitacora.toAbsolutePath().toString());
        Map<String, Object> estadoFinal = socket.consultarEstado();
        Consola.cliente("Metricas del Broker: " + Json.escribir(estadoFinal.get("metricas")));
        comprobar("19b. El monitoreo (mensaje ESTADO) expone servicios, metricas y ultimas solicitudes",
                estadoFinal.get("metricas") instanceof Map<?, ?> && estadoFinal.get("ultimasSolicitudes") instanceof List<?>,
                "ESTADO");

        // ---------------------------------------------------------------- cierre
        for (ProveedorServidor p : new ProveedorServidor[] { productos, usuarios, pedidos, pruebas }) {
            p.detener();
        }
        brokerNuevo.detener();

        Consola.titulo("RESUMEN");
        tabla.forEach(System.out::println);
        System.out.println();
        System.out.println(pasaron + " comprobaciones correctas, " + fallaron + " fallidas  (" + LocalDateTime.now().withNano(0) + ")");
        System.exit(fallaron == 0 ? 0 : 1);
    }

    private static ProveedorServidor iniciar(com.slamshoes.broker.ProveedorServicio proveedor, int puertoBroker,
                                             boolean reemplazar) throws IOException {
        // Latido de 300 ms para que las pruebas de re-registro sean rapidas
        ProveedorServidor servidor = new ProveedorServidor(proveedor, "localhost", 0, "localhost", puertoBroker, reemplazar, 300);
        servidor.iniciar();
        return servidor;
    }

    private static boolean esperar(BooleanSupplier condicion, long maxMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + maxMs;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condicion.getAsBoolean();
    }

    private static boolean fallaJson(String texto) {
        try {
            Json.leer(texto);
            return false;
        } catch (Json.JsonException e) {
            return true;
        }
    }

    private static void comprobar(String nombre, boolean cumple, String detalle) {
        if (cumple) {
            pasaron++;
        } else {
            fallaron++;
        }
        String linea = (cumple ? "[PASA]  " : "[FALLA] ") + nombre + (detalle == null ? "" : "   <" + detalle + ">");
        tabla.add(linea);
        Consola.cliente(linea);
    }
}
