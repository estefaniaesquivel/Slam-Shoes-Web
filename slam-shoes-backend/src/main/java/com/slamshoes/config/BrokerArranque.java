package com.slamshoes.config;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.red.BrokerServidor;
import com.slamshoes.broker.red.ProveedorServidor;
import com.slamshoes.proveedores.ServicioPedidosJpa;
import com.slamshoes.proveedores.ServicioProductosJpa;
import com.slamshoes.proveedores.ServicioUsuariosJpa;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Pone en marcha la parte del Broker que vive dentro de la aplicacion web:
 *
 *  1. (opcional) Un Broker local, solo si todavia no hay uno escuchando en el puerto. Si ya corres
 *     BrokerMain en otra terminal, esta aplicacion lo detecta (puerto ocupado) y usa ese.
 *  2. Los tres proveedores reales (productos, usuarios, pedidos), cada uno con su propio socket TCP,
 *     que se registran SOLOS en el Broker y repiten el aviso cada 5 s (si el Broker se reinicia,
 *     vuelven a aparecer sin reiniciar la aplicacion).
 *
 * Nada de esto detiene el arranque de la aplicacion: si algo falla solo se avisa en consola.
 */
@Component
public class BrokerArranque {

    @Value("${broker.host:localhost}") private String brokerHost;
    @Value("${broker.puerto:9000}") private int brokerPuerto;
    @Value("${broker.local.activo:true}") private boolean brokerLocalActivo;
    @Value("${broker.bitacora:bitacora-broker.log}") private String archivoBitacora;
    @Value("${broker.proveedores.puerto-base:9101}") private int puertoBase;

    @Autowired private ServicioProductosJpa productos;
    @Autowired private ServicioUsuariosJpa usuarios;
    @Autowired private ServicioPedidosJpa pedidos;

    private BrokerServidor brokerLocal;
    private final List<ProveedorServidor> servidores = new ArrayList<>();

    @PostConstruct
    public void iniciar() {
        try {
            if (brokerLocalActivo && esLocal(brokerHost)) {
                BrokerServidor candidato = new BrokerServidor(brokerPuerto, Paths.get(archivoBitacora));
                try {
                    candidato.iniciar();
                    brokerLocal = candidato;
                } catch (IOException e) {
                    Consola.broker("Ya hay un Broker en el puerto " + brokerPuerto
                            + " (" + e.getMessage() + "): se usara ese");
                }
            }
            ProveedorServicio[] reales = { productos, usuarios, pedidos };
            for (int i = 0; i < reales.length; i++) {
                ProveedorServidor servidor = new ProveedorServidor(reales[i], "localhost", puertoBase + i,
                        brokerHost, brokerPuerto, false, 5000);
                try {
                    servidor.iniciar();
                    servidores.add(servidor);
                } catch (IOException e) {
                    Consola.proveedor(reales[i].getNombreServicio(), "No se pudo abrir el puerto " + (puertoBase + i)
                            + ": " + e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            Consola.broker("No se pudo iniciar la integracion con el Broker: " + e);
        }
    }

    @PreDestroy
    public void detener() {
        for (ProveedorServidor s : servidores) {
            s.detener();
        }
        servidores.clear();
        if (brokerLocal != null) {
            brokerLocal.detener();
            brokerLocal = null;
        }
    }

    private static boolean esLocal(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }
}
