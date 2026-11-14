package com.slamshoes.nivel2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.red.Protocolo;
import com.slamshoes.broker.red.ProveedorServidor;
import com.slamshoes.providers.ServicioPedidos;
import com.slamshoes.providers.ServicioPedidosEnMemoria;
import com.slamshoes.providers.ServicioProductos;
import com.slamshoes.providers.ServicioPruebas;
import com.slamshoes.providers.ServicioUsuarios;

/**
 * PROCESOS 2, 3, 4...: un proveedor por proceso (con los datos de ejemplo en memoria del Nivel 1).
 *
 * Uso:
 *   java -cp target/nivel2 com.slamshoes.nivel2.ProveedorMain TIPO [puerto] [brokerHost] [brokerPuerto] [--reemplazar]
 *
 * TIPO: productos | usuarios | pedidos | pedidos-alterno | pruebas
 *   - "pedidos-alterno" es otra implementación del servicio "pedidos": con --reemplazar sustituye al
 *     proveedor "pedidos" que ya esté registrado, sin tocar al cliente.
 *   - "pruebas" sirve para provocar excepciones, timeouts y duplicados.
 */
public final class ProveedorMain {

    private ProveedorMain() {
    }

    private static void uso() {
        System.out.println("Uso: ProveedorMain TIPO [puerto] [brokerHost] [brokerPuerto] [--reemplazar]");
        System.out.println("  TIPO: productos | usuarios | pedidos | pedidos-alterno | pruebas");
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            uso();
            System.exit(1);
        }
        String tipo = args[0].toLowerCase();

        ProveedorServicio proveedor;
        int puertoPorOmision;
        switch (tipo) {
            case "productos":
                proveedor = new ServicioProductos();
                puertoPorOmision = 9101;
                break;
            case "usuarios":
                proveedor = new ServicioUsuarios();
                puertoPorOmision = 9102;
                break;
            case "pedidos":
                proveedor = new ServicioPedidos();
                puertoPorOmision = 9103;
                break;
            case "pedidos-alterno":
                proveedor = new ServicioPedidosEnMemoria();
                puertoPorOmision = 9104;
                break;
            case "pruebas":
                proveedor = new ServicioPruebas();
                puertoPorOmision = 9105;
                break;
            default:
                System.out.println("Tipo de proveedor desconocido: '" + args[0] + "'");
                uso();
                System.exit(1);
                return;
        }

        boolean reemplazar = false;
        List<String> posicionales = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--reemplazar")) {
                reemplazar = true;
            } else {
                posicionales.add(args[i]);
            }
        }

        int puerto = puertoPorOmision;
        String brokerHost = "localhost";
        int brokerPuerto = Protocolo.PUERTO_BROKER;
        try {
            if (posicionales.size() > 0) {
                puerto = Integer.parseInt(posicionales.get(0));
            }
            if (posicionales.size() > 1) {
                brokerHost = posicionales.get(1);
            }
            if (posicionales.size() > 2) {
                brokerPuerto = Integer.parseInt(posicionales.get(2));
            }
        } catch (NumberFormatException e) {
            System.out.println("Puerto invalido: " + e.getMessage());
            uso();
            System.exit(1);
        }

        ProveedorServidor servidor = new ProveedorServidor(proveedor, "localhost", puerto, brokerHost, brokerPuerto,
                reemplazar, 5000);
        try {
            servidor.iniciar();
        } catch (IOException e) {
            System.out.println("No se pudo abrir el puerto " + puerto + ": " + e.getMessage());
            System.exit(1);
        }
        Consola.proveedor(proveedor.getNombreServicio(), "Listo. Ctrl+C para detener (se dara de baja en el Broker).");

        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
        Thread.currentThread().join();
    }
}
