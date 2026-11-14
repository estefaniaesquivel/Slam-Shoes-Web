package com.slamshoes.broker;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Directorio que asocia un nombre de servicio con el proveedor que lo atiende.
 *
 * Permite registrar, buscar y sustituir proveedores sin que el cliente se entere.
 * Usa un ConcurrentHashMap para que en el Nivel 2 pueda consultarse desde varios hilos.
 */
public class RegistroServicios {

    private final Map<String, ProveedorServicio> proveedores = new ConcurrentHashMap<>();

    /**
     * Da de alta un proveedor.
     *
     * @throws IllegalArgumentException si el nombre ya está registrado (usa {@link #sustituir})
     */
    public void registrar(String nombre, ProveedorServicio proveedor) {
        validar(nombre, proveedor);
        ProveedorServicio previo = proveedores.putIfAbsent(nombre, proveedor);
        if (previo != null) {
            Consola.registro("RECHAZADO: ya existe un proveedor para '" + nombre + "' ("
                    + nombreDe(previo) + "). Usa sustituir().");
            throw new IllegalArgumentException("Ya existe un proveedor registrado para '" + nombre + "'");
        }
        Consola.registro("Proveedor registrado: " + nombre + " -> " + nombreDe(proveedor));
    }

    /** Busca el proveedor de un servicio; vacío si nadie lo registró. */
    public Optional<ProveedorServicio> buscar(String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(proveedores.get(nombre));
    }

    public boolean existe(String nombre) {
        return buscar(nombre).isPresent();
    }

    /**
     * Cambia quién atiende un servicio ya registrado. El cliente sigue pidiendo
     * el mismo nombre de servicio y no necesita ningún cambio.
     *
     * @return el proveedor anterior
     * @throws IllegalArgumentException si el servicio no estaba registrado
     */
    public ProveedorServicio sustituir(String nombre, ProveedorServicio nuevo) {
        validar(nombre, nuevo);
        ProveedorServicio anterior = proveedores.replace(nombre, nuevo);
        if (anterior == null) {
            throw new IllegalArgumentException("No se puede sustituir: el servicio '" + nombre + "' no esta registrado");
        }
        Consola.registro("Proveedor sustituido: " + nombre + " -> " + nombreDe(anterior)
                + " reemplazado por " + nombreDe(nuevo));
        return anterior;
    }

    /**
     * Da de baja un servicio, pero solo si el proveedor registrado sigue siendo el mismo
     * (así un proveedor viejo no puede borrar al que lo sustituyó).
     *
     * @return true si se eliminó
     */
    public boolean eliminar(String nombre, ProveedorServicio esperado) {
        if (nombre == null || esperado == null) {
            return false;
        }
        boolean quitado = proveedores.remove(nombre, esperado);
        if (quitado) {
            Consola.registro("Proveedor dado de baja: " + nombre + " (" + nombreDe(esperado) + ")");
        }
        return quitado;
    }

    /** Nombres de los servicios registrados, ordenados alfabéticamente. */
    public Set<String> listarNombres() {
        return new TreeSet<>(proveedores.keySet());
    }

    private static void validar(String nombre, ProveedorServicio proveedor) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio");
        }
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio");
        }
    }

    private static String nombreDe(ProveedorServicio proveedor) {
        return proveedor.getClass().getSimpleName();
    }
}
