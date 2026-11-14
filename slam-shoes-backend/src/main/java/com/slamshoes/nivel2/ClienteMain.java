package com.slamshoes.nivel2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.EstadoRespuesta;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.broker.cliente.ClienteBroker;
import com.slamshoes.broker.red.ClienteSocket;
import com.slamshoes.broker.red.Protocolo;

/**
 * PROCESO CLIENTE del Nivel 2: construye solicitudes, las manda al Broker por un socket y muestra
 * la respuesta. NO importa ninguna clase de com.slamshoes.providers: no sabe quién atiende nada.
 *
 * Uso:  java -cp target/nivel2 com.slamshoes.nivel2.ClienteMain [brokerHost=localhost] [brokerPuerto=9000] [--con-timeout]
 *
 * Requiere el Broker y los proveedores productos, usuarios, pedidos y pruebas ya corriendo.
 * En el escenario 6 se detiene y pide que inicies el proveedor alterno (pausa con ENTER).
 */
public final class ClienteMain {

    private static int ok = 0;
    private static int mal = 0;
    private static final List<String> resumen = new ArrayList<>();

    private ClienteMain() {
    }

    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int puerto = Protocolo.PUERTO_BROKER;
        boolean conTimeout = false;
        List<String> posicionales = new ArrayList<>();
        for (String a : args) {
            if (a.equals("--con-timeout")) {
                conTimeout = true;
            } else {
                posicionales.add(a);
            }
        }
        try {
            if (posicionales.size() > 0) {
                host = posicionales.get(0);
            }
            if (posicionales.size() > 1) {
                puerto = Integer.parseInt(posicionales.get(1));
            }
        } catch (NumberFormatException e) {
            System.out.println("Puerto invalido. Uso: ClienteMain [brokerHost] [brokerPuerto] [--con-timeout]");
            System.exit(1);
        }

        ClienteSocket socket = new ClienteSocket(host, puerto);
        ClienteBroker cliente = new ClienteBroker(socket); // el mismo cliente del Nivel 1, ahora sobre sockets
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        Consola.titulo("1 y 2. Proveedores registrados y localizacion de servicios");
        Map<String, Object> estado;
        try {
            estado = socket.consultarEstado();
        } catch (IOException e) {
            System.out.println("No se pudo contactar al Broker en " + host + ":" + puerto + ": " + e.getMessage());
            System.out.println("Inicia primero: BrokerMain y los proveedores (ver README_NIVEL2.md).");
            System.exit(1);
            return;
        }
        mostrarServicios(estado);

        Consola.titulo("3 y 4. Ejecucion de una operacion y respuesta exitosa");
        verificar("Operacion exitosa (productos.obtenerDetalle)",
                cliente.enviar("productos", "obtenerDetalle", Map.of("idProducto", 1)), EstadoRespuesta.EXITO);

        Consola.titulo("5. Servicio desconocido");
        verificar("Servicio inexistente 'envios'",
                cliente.enviar("envios", "calcularCosto", Map.of("codigoPostal", "97000")),
                EstadoRespuesta.SERVICIO_NO_ENCONTRADO);

        Consola.titulo("6. Parametro invalido");
        verificar("idProducto = 'abc'",
                cliente.enviar("productos", "obtenerDetalle", Map.of("idProducto", "abc")),
                EstadoRespuesta.PARAMETROS_INVALIDOS);
        verificar("Falta idProducto", cliente.enviar("productos", "obtenerDetalle", Map.of()),
                EstadoRespuesta.PARAMETROS_INVALIDOS);

        Consola.titulo("7. Excepcion producida por un proveedor");
        verificar("pruebas.fallar lanza IllegalStateException", cliente.enviar("pruebas", "fallar", Map.of()),
                EstadoRespuesta.ERROR_PROVEEDOR);
        Consola.cliente("El Broker sigue vivo despues del fallo:");
        verificar("productos.consultarStock tras el fallo",
                cliente.enviar("productos", "consultarStock", Map.of("idVariante", 101)), EstadoRespuesta.EXITO);

        Consola.titulo("8. Cambio de proveedor sin modificar el cliente");
        Consola.cliente("Antes de sustituir (misma solicitud que se repetira despues):");
        Respuesta antes = cliente.enviar("pedidos", "consultarHistorial", Map.of("idUsuario", 1));
        verificar("pedidos antes del cambio", antes, EstadoRespuesta.EXITO);
        System.out.println();
        System.out.println(">>> En OTRA terminal inicia el proveedor alterno:");
        System.out.println(">>>   java -cp target/nivel2 com.slamshoes.nivel2.ProveedorMain pedidos-alterno 9104 localhost "
                + puerto + " --reemplazar");
        System.out.println(">>> Cuando veas 'Registrado en el Broker ... sustituido', presiona ENTER aqui.");
        if (teclado.readLine() == null) {
            System.out.println("(sin teclado: se continua)");
        }
        Consola.cliente("Despues de sustituir (MISMA solicitud, MISMO cliente):");
        Respuesta despues = cliente.enviar("pedidos", "consultarHistorial", Map.of("idUsuario", 1));
        String antesQuien = quienAtendio(antes);
        String despuesQuien = quienAtendio(despues);
        verificarCondicion("Cambio de proveedor: " + antesQuien + " -> " + despuesQuien,
                despues.isExitosa() && !antesQuien.equals(despuesQuien));

        Consola.titulo("Extra A. Solicitud duplicada (mismo idSolicitud enviado dos veces)");
        Solicitud repetida = new Solicitud("pruebas", "contador", Map.of());
        Respuesta r1 = socket.procesar(repetida);
        Respuesta r2 = socket.procesar(repetida);
        Consola.cliente("Primera respuesta:  " + r1.getResultado());
        Consola.cliente("Segunda respuesta:  " + r2.getResultado());
        verificarCondicion("La operacion se ejecuto UNA sola vez (ambas respuestas iguales)",
                r1.isExitosa() && r2.isExitosa() && String.valueOf(r1.getResultado()).equals(String.valueOf(r2.getResultado())));

        if (conTimeout) {
            Consola.titulo("Extra B. Timeout (el proveedor tarda 8 segundos, el limite es 5)");
            Consola.cliente("Esto tarda unos 17 segundos (3 intentos del Broker)...");
            verificar("pruebas.dormir 8000 ms", cliente.enviar("pruebas", "dormir", Map.of("ms", 8000)),
                    EstadoRespuesta.TIEMPO_AGOTADO);
        } else {
            Consola.titulo("Extra B. Timeout: omitido (agrega --con-timeout para probarlo)");
        }

        Consola.titulo("Resultado");
        resumen.forEach(System.out::println);
        System.out.println();
        System.out.println(ok + " verificaciones correctas, " + mal + " fallidas");
        if (mal > 0) {
            System.exit(1);
        }
    }

    private static void mostrarServicios(Map<String, Object> estado) {
        Object servicios = estado.get("servicios");
        if (servicios instanceof List<?> lista) {
            Consola.cliente("Servicios registrados en el Broker: " + lista.size());
            for (Object o : lista) {
                if (o instanceof Map<?, ?> s) {
                    Consola.cliente("  - " + s.get("servicio") + " -> " + s.get("host") + ":" + s.get("puerto")
                            + "  operaciones=" + s.get("operaciones"));
                }
            }
        }
        verificarCondicion("Hay proveedores registrados dinamicamente",
                servicios instanceof List<?> l && !l.isEmpty());
    }

    private static String quienAtendio(Respuesta r) {
        if (r.getResultado() instanceof Map<?, ?> m && m.get("atendidoPor") != null) {
            return m.get("atendidoPor").toString();
        }
        return "?";
    }

    private static void verificar(String nombre, Respuesta respuesta, EstadoRespuesta esperado) {
        verificarCondicion(nombre + " -> " + respuesta.getEstado() + " (se esperaba " + esperado + ")",
                respuesta.getEstado() == esperado);
    }

    private static void verificarCondicion(String descripcion, boolean cumple) {
        if (cumple) {
            ok++;
        } else {
            mal++;
        }
        String linea = (cumple ? "[OK]    " : "[FALLO] ") + descripcion;
        resumen.add(linea);
        Consola.cliente(linea);
    }
}
