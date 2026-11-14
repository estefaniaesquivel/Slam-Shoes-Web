package com.slamshoes.nivel2;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.red.BrokerServidor;
import com.slamshoes.broker.red.Protocolo;

/**
 * PROCESO 1 del Nivel 2: el Broker.
 *
 * Uso:  java -cp target/nivel2 com.slamshoes.nivel2.BrokerMain [puerto=9000] [bitacora=bitacora-broker.log]
 *
 * No usa Spring ni base de datos. Los proveedores se registran solos cuando se inician.
 */
public final class BrokerMain {

    private BrokerMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        int puerto = Protocolo.PUERTO_BROKER;
        Path bitacora = Paths.get(args.length > 1 ? args[1] : "bitacora-broker.log");
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Puerto invalido: '" + args[0] + "'. Uso: BrokerMain [puerto] [archivoBitacora]");
                System.exit(1);
            }
        }

        BrokerServidor broker = new BrokerServidor(puerto, bitacora);
        try {
            broker.iniciar();
        } catch (IOException e) {
            System.out.println("No se pudo abrir el puerto " + puerto + ": " + e.getMessage()
                    + " (quiza ya hay un Broker corriendo)");
            System.exit(1);
        }
        Consola.broker("Bitacora en: " + bitacora.toAbsolutePath());
        Consola.broker("Esperando proveedores y clientes. Ctrl+C para detener.");

        Runtime.getRuntime().addShutdownHook(new Thread(broker::detener));
        Thread.currentThread().join(); // mantiene vivo el proceso
    }
}
