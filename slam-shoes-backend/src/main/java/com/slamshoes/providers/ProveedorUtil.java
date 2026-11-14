package com.slamshoes.providers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidades compartidas por los proveedores: armar mapas de resultado y
 * leer parámetros con validación. Todos los errores de validación se lanzan
 * como IllegalArgumentException y cada proveedor los convierte en una
 * respuesta PARAMETROS_INVALIDOS.
 */
public final class ProveedorUtil {

    private ProveedorUtil() {
    }

    /** Arma un mapa que conserva el orden: mapa("a", 1, "b", 2) -> {a=1, b=2}. */
    public static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            resultado.put((String) pares[i], pares[i + 1]);
        }
        return resultado;
    }

    /** Convierte un valor (número o texto numérico) a long, o lanza IllegalArgumentException. */
    public static long convertirALong(Object valor, String nombre) {
        if (valor == null) {
            throw new IllegalArgumentException("Falta el parametro '" + nombre + "'");
        }
        if (valor instanceof Number numero) {
            double decimal = numero.doubleValue();
            if (decimal != Math.rint(decimal)) {
                throw new IllegalArgumentException(
                        "El parametro '" + nombre + "' debe ser un numero entero, se recibio '" + valor + "'");
            }
            return numero.longValue();
        }
        try {
            return Long.parseLong(valor.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El parametro '" + nombre + "' debe ser un numero entero, se recibio '" + valor + "'");
        }
    }

    public static long obtenerLong(Map<String, Object> parametros, String nombre) {
        return convertirALong(parametros.get(nombre), nombre);
    }

    public static String obtenerTexto(Map<String, Object> parametros, String nombre) {
        Object valor = parametros.get(nombre);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Falta el parametro '" + nombre + "'");
        }
        return valor.toString().trim();
    }

    /**
     * Valida el parámetro "items" de crearPedido (lista de mapas con idVariante y
     * cantidad mayor que cero) y devuelve {número de líneas, total de unidades}.
     */
    public static int[] resumirItems(Map<String, Object> parametros) {
        Object crudo = parametros.get("items");
        if (!(crudo instanceof List<?> items) || items.isEmpty()) {
            throw new IllegalArgumentException("El parametro 'items' debe ser una lista con al menos un elemento");
        }
        int unidades = 0;
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> linea)) {
                throw new IllegalArgumentException("Cada elemento de 'items' debe tener idVariante y cantidad");
            }
            convertirALong(linea.get("idVariante"), "items.idVariante");
            long cantidad = convertirALong(linea.get("cantidad"), "items.cantidad");
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero, se recibio " + cantidad);
            }
            unidades += cantidad;
        }
        return new int[] { items.size(), unidades };
    }
}
