package com.slamshoes.broker;

import java.util.Objects;
import java.util.Optional;

/**
 * Intermediario entre el cliente y los proveedores.
 *
 * Por cada solicitud: la valida, identifica servicio y operación, busca el
 * proveedor en el {@link RegistroServicios}, se la reenvía, recibe el resultado
 * y lo devuelve. Nunca lanza excepciones hacia el cliente: todo problema se
 * convierte en una {@link Respuesta} con un {@link EstadoRespuesta} de error.
 */
public class BrokerCentral implements Broker {

    private final RegistroServicios registro;

    public BrokerCentral(RegistroServicios registro) {
        this.registro = Objects.requireNonNull(registro, "El registro de servicios es obligatorio");
    }

    @Override
    public Respuesta procesar(Solicitud solicitud) {
        // 1. Recibir y validar
        if (solicitud == null) {
            Consola.broker("Solicitud nula rechazada");
            return Respuesta.error("sin-id", EstadoRespuesta.SOLICITUD_INVALIDA, "La solicitud no puede ser nula");
        }
        String id = solicitud.getIdSolicitud();
        String corto = solicitud.getIdCorto();
        String servicio = solicitud.getServicio();
        String operacion = solicitud.getOperacion();

        // 2. Identificar la operación requerida
        Consola.broker("Solicitud " + corto + " recibida: " + servicio + "." + operacion);

        // 3. Buscar un proveedor en el registro
        Optional<ProveedorServicio> encontrado = registro.buscar(servicio);
        if (encontrado.isEmpty()) {
            Consola.broker("Solicitud " + corto + ": no hay proveedor para '" + servicio + "'");
            return terminar(corto, Respuesta.error(id, EstadoRespuesta.SERVICIO_NO_ENCONTRADO,
                    "No existe un proveedor registrado para el servicio '" + servicio + "'"));
        }
        ProveedorServicio proveedor = encontrado.get();
        String nombreProveedor = proveedor.getClass().getSimpleName();
        Consola.broker("Proveedor localizado: " + nombreProveedor);

        // Rechazar operaciones que el proveedor no ofrece
        if (!proveedor.getOperacionesSoportadas().contains(operacion)) {
            Consola.broker("Solicitud " + corto + ": " + nombreProveedor + " no ofrece '" + operacion + "'");
            return terminar(corto, Respuesta.error(id, EstadoRespuesta.OPERACION_NO_SOPORTADA,
                    "El servicio '" + servicio + "' no ofrece la operacion '" + operacion
                            + "'. Operaciones disponibles: " + proveedor.getOperacionesSoportadas()));
        }

        // 4. Reenviar la solicitud y 5. recibir el resultado
        Respuesta respuesta;
        try {
            Consola.broker("Reenviando " + corto + " a " + nombreProveedor);
            respuesta = proveedor.ejecutar(solicitud);
        } catch (Exception e) {
            Consola.broker("EXCEPCION en " + nombreProveedor + ": " + e);
            return terminar(corto, Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR,
                    "El proveedor fallo: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
        }

        // El contrato exige una respuesta con el mismo identificador
        if (respuesta == null) {
            return terminar(corto, Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR,
                    nombreProveedor + " devolvio una respuesta nula"));
        }
        if (!id.equals(respuesta.getIdSolicitud())) {
            return terminar(corto, Respuesta.error(id, EstadoRespuesta.ERROR_PROVEEDOR,
                    nombreProveedor + " devolvio una respuesta con otro idSolicitud"));
        }

        // 6. Devolver la respuesta al cliente
        return terminar(corto, respuesta);
    }

    private Respuesta terminar(String corto, Respuesta respuesta) {
        Consola.broker("Respuesta " + corto + " estado=" + respuesta.getEstado());
        return respuesta;
    }
}
