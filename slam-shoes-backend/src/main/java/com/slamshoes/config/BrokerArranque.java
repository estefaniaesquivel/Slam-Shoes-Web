package com.slamshoes.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.slamshoes.broker.Consola;
import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.http.ExposicionProveedores;
import com.slamshoes.broker.http.RegistroAutomatico;

import jakarta.annotation.PreDestroy;

/**
 * Auto-registro de los proveedores de esta aplicación en el Broker, por HTTP, cuando la aplicación ya está lista
 * ({@link ApplicationReadyEvent}: el servidor web ya atiende peticiones, así que el Broker puede contestar de vuelta).
 *
 * Variables de entorno (todas opcionales en desarrollo):
 * <ul>
 *   <li>{@code BROKER_URL}: URL HTTPS del Broker desplegado. Por omisión, esta misma aplicación.</li>
 *   <li>{@code SERVICE_PUBLIC_URL}: URL pública con la que el Broker alcanza a ESTA aplicación; sirve aunque Render
 *       asigne cualquier puerto interno ({@code $PORT}). Por omisión {@code http://localhost:<server.port>}.</li>
 *   <li>{@code BROKER_TOKEN}: secreto compartido opcional entre Broker y proveedores.</li>
 * </ul>
 *
 * Nada de esto detiene el arranque: si el Broker no responde, el registro se reintenta solo cada segundo.
 *
 * REFACTOR: Extract Function / Substitute Algorithm
 * RAZÓN: el arranque anterior abría tres puertos TCP (9101-9103) y un Broker local en el 9000, y fallaba o se
 * quedaba mudo cuando un puerto estaba ocupado o bloqueado. Ahora solo calcula una URL pública y delega en
 * {@link RegistroAutomatico}, que hace un POST HTTP: no hay puertos propios que abrir.
 */
@Component
@ConditionalOnProperty(name = "broker.proveedores.activo", havingValue = "true", matchIfMissing = true)
public class BrokerArranque {

    /** Raíz REST de cada servicio para el proxy /api/gateway/{servicio}/**. */
    // REFACTOR: Replace Magic Literal
    // RAZON: el proxy necesita saber la ruta REST de cada servicio. Antes los puertos 9101, 9102 y 9103 eran
    // numeros sueltos en el codigo; ahora el mapa servicio -> ruta es una constante con nombre y es el unico lugar
    // que hay que editar al agregar un servicio.
    private static final Map<String, String> RUTA_REST = Map.of(
            "productos", "/api/productos",
            "usuarios", "/api/usuarios",
            "pedidos", "/api/pedidos");

    private final ExposicionProveedores exposicion;
    private final String urlBroker;
    private final String urlPublica;
    private final String token;
    private final boolean reemplazar;
    private final long msLatido;

    private final List<RegistroAutomatico> registros = new ArrayList<>();

    // REFACTOR: Change Function Declaration
    // RAZON: antes los datos llegaban por campos {@code @Value} y {@code @Autowired} de las clases concretas
    // ServicioProductosJpa, ServicioUsuariosJpa y ServicioPedidosJpa. Ahora el constructor recibe
    // ExposicionProveedores y las URLs, de modo que la clase no conoce proveedores concretos y se puede crear en
    // una prueba sin Spring.
    public BrokerArranque(ExposicionProveedores exposicion,
                          @Value("${broker.url:http://localhost:${server.port:8080}}") String urlBroker,
                          @Value("${service.public.url:http://localhost:${server.port:8080}}") String urlPublica,
                          @Value("${broker.token:}") String token,
                          @Value("${broker.proveedores.reemplazar:false}") boolean reemplazar,
                          @Value("${broker.latido-ms:15000}") long msLatido) {
        this.exposicion = exposicion;
        this.urlBroker = urlBroker;
        this.urlPublica = urlPublica;
        this.token = token;
        this.reemplazar = reemplazar;
        this.msLatido = msLatido;
    }

    /** Se ejecuta una vez, cuando la aplicación terminó de arrancar. */
    @EventListener(ApplicationReadyEvent.class)
    // REFACTOR: Extract Function
    // RAZON: el {@code iniciar()} anterior levantaba un Broker local, comprobaba puertos ocupados y abria tres
    // servidores. Ahora el metodo solo recorre los proveedores y le pide a cada {@code RegistroAutomatico} que se
    // anuncie; el trabajo de red salio a esa clase.
    public void iniciar() {
        for (ProveedorServicio proveedor : exposicion.getProveedores()) {
            try {
                RegistroAutomatico registro = new RegistroAutomatico(proveedor, urlPublica,
                        RUTA_REST.getOrDefault(proveedor.getNombreServicio(), ""), urlBroker, token, reemplazar, msLatido);
                registro.iniciar();
                registros.add(registro);
            } catch (RuntimeException e) {
                Consola.proveedor(proveedor.getNombreServicio(),
                        "No se pudo iniciar el auto-registro (revisa BROKER_URL y SERVICE_PUBLIC_URL): " + e.getMessage());
            }
        }
    }

    @PreDestroy
    public void detener() {
        for (RegistroAutomatico registro : registros) {
            registro.detener();
        }
        registros.clear();
    }
}
