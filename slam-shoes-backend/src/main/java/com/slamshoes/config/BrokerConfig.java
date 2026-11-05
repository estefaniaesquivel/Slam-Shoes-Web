package com.slamshoes.config;

import com.slamshoes.broker.RegistroServicios;
import com.slamshoes.providers.ServicioPedidos;
import com.slamshoes.providers.ServicioPedidosEnMemoria;
import com.slamshoes.providers.ServicioProductos;
import com.slamshoes.providers.ServicioUsuarios;

/**
 * Único lugar que conoce las clases concretas de los proveedores: los da de alta
 * en el registro. El cliente y el Broker nunca las importan.
 *
 * NOTA (Nivel 1): es una clase normal de Java, sin anotaciones de Spring, para
 * poder usarla desde la demo de consola sin tocar el servidor. Más adelante, al
 * integrar el Broker con la aplicación web, se convierte en una clase
 * @Configuration que crea el registro y el BrokerCentral como beans.
 */
public final class BrokerConfig {

    private BrokerConfig() {
    }

    /** Registra los tres servicios mínimos: productos, usuarios y pedidos. */
    public static void registrarProveedores(RegistroServicios registro) {
        registro.registrar("productos", new ServicioProductos());
        registro.registrar("usuarios", new ServicioUsuarios());
        registro.registrar("pedidos", new ServicioPedidos());
    }

    /** Cambia el proveedor de "pedidos" por la versión alterna, sin tocar al cliente. */
    public static void sustituirPedidosPorEnMemoria(RegistroServicios registro) {
        registro.sustituir("pedidos", new ServicioPedidosEnMemoria());
    }
}
