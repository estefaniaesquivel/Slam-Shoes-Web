package com.slamshoes.broker.cliente;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slamshoes.broker.Broker;
import com.slamshoes.broker.BrokerCentral;
import com.slamshoes.broker.Consola;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.RegistroServicios;
import com.slamshoes.broker.Respuesta;
import com.slamshoes.broker.Solicitud;
import com.slamshoes.config.BrokerConfig;

/**
 * Demo de consola del Broker (Nivel 1). Corre aparte del servidor web:
 * no usa Spring, Tomcat, puertos ni base de datos.
 *
 * Muestra, en este orden: registro de tres servicios, una operacion por servicio,
 * servicio inexistente, parametros invalidos, excepcion de un proveedor y
 * sustitucion de un proveedor sin tocar al cliente.
 */
public class DemoBroker {

    /** Proveedor de pruebas que solo sirve para provocar errores controlados. */
    private static class ProveedorDePrueba implements ProveedorServicio {
        private final String nombre;
        private final boolean debeFallar;

        ProveedorDePrueba(String nombre, boolean debeFallar) {
            this.nombre = nombre;
            this.debeFallar = debeFallar;
        }

        @Override
        public Respuesta ejecutar(Solicitud solicitud) {
            if (debeFallar) {
                throw new IllegalStateException("fallo simulado dentro del proveedor de pruebas");
            }
            return Respuesta.exito(solicitud.getIdSolicitud(), Map.of("atendidoPor", "ProveedorDePrueba"));
        }

        @Override
        public String getNombreServicio() {
            return nombre;
        }

        @Override
        public Set<String> getOperacionesSoportadas() {
            return Set.of("fallar");
        }
    }

    public static void main(String[] args) {
        RegistroServicios registro = new RegistroServicios();
        Broker broker = new BrokerCentral(registro);
        ClienteBroker cliente = new ClienteBroker(broker);

        // 1. Los proveedores se registran
        Consola.titulo("1. Registro de tres servicios");
        BrokerConfig.registrarProveedores(registro);
        Consola.registro("Servicios disponibles: " + registro.listarNombres());
        try {
            registro.registrar("pedidos", new ProveedorDePrueba("pedidos", false)); // duplicado
        } catch (IllegalArgumentException e) {
            Consola.registro("(el segundo registro de 'pedidos' fue rechazado, como se esperaba)");
        }

        // 2. Una operacion por servicio
        Consola.titulo("2. Una operacion por servicio");
        cliente.enviar("productos", "obtenerDetalle", Map.of("idProducto", 1));
        System.out.println();
        cliente.enviar("usuarios", "autenticar", Map.of("correo", "demo@slamshoes.com", "contrasena", "demo123"));
        System.out.println();
        cliente.enviar("pedidos", "consultarHistorial", Map.of("idUsuario", 1));

        // 3. Servicio inexistente
        Consola.titulo("3. Servicio inexistente");
        cliente.enviar("envios", "calcularCosto", Map.of("codigoPostal", "97000"));

        // 4. Parametros invalidos (y una operacion que no existe)
        Consola.titulo("4. Parametros invalidos");
        cliente.enviar("productos", "obtenerDetalle", Map.of("idProducto", "abc"));
        System.out.println();
        cliente.enviar("productos", "obtenerDetalle", Map.of());
        System.out.println();
        cliente.enviar("pedidos", "crearPedido", Map.of("idUsuario", 1, "direccionEnvio", "Calle 60 #123, Merida",
                "items", List.of(Map.of("idVariante", 101, "cantidad", 0))));
        System.out.println();
        cliente.enviar("productos", "borrarCatalogo", Map.of());

        // 5. Excepcion producida por un proveedor
        Consola.titulo("5. Excepcion dentro de un proveedor");
        registro.registrar("pruebas", new ProveedorDePrueba("pruebas", true));
        cliente.enviar("pruebas", "fallar", Map.of());
        System.out.println();
        Consola.cliente("El Broker sigue funcionando despues del fallo:");
        cliente.enviar("productos", "consultarStock", Map.of("idVariante", 101));

        // 6. Sustitucion de un proveedor sin modificar el cliente
        Consola.titulo("6. Sustitucion de un proveedor (el cliente no cambia)");
        Consola.cliente("Antes de sustituir:");
        cliente.enviar("pedidos", "consultarHistorial", Map.of("idUsuario", 1));
        System.out.println();
        BrokerConfig.sustituirPedidosPorEnMemoria(registro);
        System.out.println();
        Consola.cliente("Despues de sustituir (misma solicitud):");
        cliente.enviar("pedidos", "consultarHistorial", Map.of("idUsuario", 1));

        Consola.titulo("Fin de la demo");
    }
}

//NOTAS PARA COMPILAR EL NIVEL 1:
// cd slam-shoes-backend
//mkdir target\demo -Force
//javac -encoding UTF-8 -d target\demo (Get-ChildItem -Recurse src\main\java\com\slamshoes\broker, src\main\java\com\slamshoes\providers -Filter *.java | ForEach-Object FullName) src\main\java\com\slamshoes\config\BrokerConfig.java
//java -cp target\demo com.slamshoes.broker.cliente.DemoBroker | Tee-Object evidencia-nivel1.txt