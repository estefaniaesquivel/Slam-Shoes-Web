package com.slamshoes.broker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mensajes básicos de consola del Broker: una línea por evento con prefijo
 * [CLIENTE], [BROKER] o [REGISTRO].
 *
 * Los textos que se imprimen no llevan acentos a propósito, para que se vean
 * bien en cualquier terminal de Windows.
 */
public final class Consola {

    private Consola() {
    }

    public static void cliente(String mensaje) {
        System.out.println("[CLIENTE]  " + mensaje);
    }

    public static void broker(String mensaje) {
        System.out.println("[BROKER]   " + mensaje);
    }

    public static void registro(String mensaje) {
        System.out.println("[REGISTRO] " + mensaje);
    }

    /** Encabezado para separar los escenarios de la demo. */
    public static void titulo(String texto) {
        System.out.println();
        System.out.println("==== " + texto + " ====");
    }

    /** Primeros 8 caracteres de un UUID. */
    public static String corto(String id) {
        if (id == null) {
            return "sin-id";
        }
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    /** Convierte un resultado (mapa, lista o valor simple) en líneas legibles con sangría. */
    public static List<String> aLineas(Object valor) {
        List<String> lineas = new ArrayList<>();
        agregar(valor, "", lineas);
        return lineas;
    }

    private static void agregar(Object valor, String sangria, List<String> salida) {
        if (valor instanceof Map<?, ?> mapa) {
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                Object contenido = entrada.getValue();
                if (contenido instanceof Map<?, ?> || contenido instanceof List<?>) {
                    salida.add(sangria + entrada.getKey() + ":");
                    agregar(contenido, sangria + "  ", salida);
                } else {
                    salida.add(sangria + entrada.getKey() + ": " + contenido);
                }
            }
        } else if (valor instanceof List<?> lista) {
            if (lista.isEmpty()) {
                salida.add(sangria + "(vacio)");
            }
            for (Object elemento : lista) {
                salida.add(sangria + "- " + elemento);
            }
        } else {
            salida.add(sangria + valor);
        }
    }
}
