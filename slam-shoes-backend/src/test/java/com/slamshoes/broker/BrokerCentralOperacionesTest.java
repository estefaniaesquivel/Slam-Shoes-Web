package com.slamshoes.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas del cambio en {@link BrokerCentral}: un proveedor sin lista de operaciones no se pre-valida. */
class BrokerCentralOperacionesTest {

    private static ProveedorServicio proveedor(Set<String> operaciones) {
        return new ProveedorServicio() {
            @Override
            public Respuesta ejecutar(Solicitud s) {
                return Respuesta.exito(s.getIdSolicitud(), "ok");
            }

            @Override
            public String getNombreServicio() {
                return "catalogo";
            }

            @Override
            public Set<String> getOperacionesSoportadas() {
                return operaciones;
            }
        };
    }

    @Test
    @DisplayName("Con operaciones declaradas, una operacion desconocida se rechaza")
    void validaCuandoHayLista() {
        RegistroServicios registro = new RegistroServicios();
        registro.registrar("catalogo", proveedor(Set.of("listarProductos")));
        Respuesta r = new BrokerCentral(registro).procesar(new Solicitud("catalogo", "otra", null));
        assertEquals(EstadoRespuesta.OPERACION_NO_SOPORTADA, r.getEstado());
    }

    @Test
    @DisplayName("Sin operaciones declaradas (registro minimo {servicio,url}) la solicitud se reenvia al proveedor")
    void noValidaSinLista() {
        RegistroServicios registro = new RegistroServicios();
        registro.registrar("catalogo", proveedor(Set.of()));
        Respuesta r = new BrokerCentral(registro).procesar(new Solicitud("catalogo", "cualquiera", null));
        assertEquals(EstadoRespuesta.EXITO, r.getEstado());
    }
}
