package com.slamshoes.broker.red;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convertidor JSON mínimo escrito a mano (sin librerías externas), para que el Broker, los
 * proveedores y el cliente puedan compilarse y ejecutarse solo con el JDK.
 *
 * Aquí ocurre la parte "texto" del marshalling:
 * <ul>
 *   <li>{@link #escribir(Object)}: objetos Java (Map, List, String, números, booleanos, null,
 *       fechas) -> texto JSON de UNA sola línea (los saltos de línea dentro de textos se escapan,
 *       así que una línea = un mensaje).</li>
 *   <li>{@link #leer(String)}: texto JSON -> LinkedHashMap, ArrayList, String, Long, Double,
 *       Boolean o null.</li>
 * </ul>
 */
public final class Json {

    /** Error de sintaxis al leer JSON. */
    public static class JsonException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JsonException(String mensaje) {
            super(mensaje);
        }
    }

    private static final int PROFUNDIDAD_MAXIMA = 64;

    private Json() {
    }

    // ------------------------------------------------------------------ escritura

    /** Convierte un valor Java en texto JSON (una sola línea). */
    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder(128);
        escribir(valor, sb, 0);
        return sb.toString();
    }

    private static void escribir(Object v, StringBuilder sb, int nivel) {
        if (nivel > PROFUNDIDAD_MAXIMA) {
            throw new JsonException("Estructura demasiado profunda (posible referencia circular)");
        }
        if (v == null) {
            sb.append("null");
        } else if (v instanceof CharSequence || v instanceof Character) {
            cadena(v.toString(), sb);
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Number n) {
            numero(n, sb);
        } else if (v instanceof LocalDateTime fecha) {
            cadena(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(fecha), sb);
        } else if (v instanceof TemporalAccessor || v instanceof Enum<?>) {
            cadena(v instanceof Enum<?> e ? e.name() : v.toString(), sb);
        } else if (v instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                cadena(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escribir(e.getValue(), sb, nivel + 1);
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> iterable) {
            sb.append('[');
            boolean primero = true;
            for (Object elemento : iterable) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                escribir(elemento, sb, nivel + 1);
            }
            sb.append(']');
        } else if (v.getClass().isArray()) {
            sb.append('[');
            int largo = Array.getLength(v);
            for (int i = 0; i < largo; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                escribir(Array.get(v, i), sb, nivel + 1);
            }
            sb.append(']');
        } else {
            cadena(v.toString(), sb);
        }
    }

    private static void numero(Number n, StringBuilder sb) {
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            // JSON no tiene NaN ni infinito
            sb.append(Double.isNaN(d) || Double.isInfinite(d) ? "null" : Double.toString(d));
        } else if (n instanceof java.math.BigDecimal bd) {
            sb.append(bd.toPlainString());
        } else if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte
                || n instanceof java.math.BigInteger || n instanceof java.util.concurrent.atomic.AtomicLong
                || n instanceof java.util.concurrent.atomic.AtomicInteger) {
            sb.append(n.toString());
        } else {
            double d = n.doubleValue();
            sb.append(Double.isNaN(d) || Double.isInfinite(d) ? "null" : Double.toString(d));
        }
    }

    private static void cadena(String texto, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------ lectura

    /** Convierte texto JSON en Map / List / String / Long / Double / Boolean / null. */
    public static Object leer(String texto) {
        if (texto == null) {
            throw new JsonException("El texto JSON es nulo");
        }
        Lector lector = new Lector(texto);
        Object valor = lector.valor(0);
        lector.espacios();
        if (lector.i < texto.length()) {
            throw lector.error("Hay texto sobrante despues del JSON");
        }
        return valor;
    }

    /** Lee un objeto JSON ({...}); lanza JsonException si el texto es otra cosa. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> leerObjeto(String texto) {
        Object valor = leer(texto);
        if (!(valor instanceof Map)) {
            throw new JsonException("Se esperaba un objeto JSON ({...})");
        }
        return (Map<String, Object>) valor;
    }

    private static final class Lector {
        private final String s;
        private int i = 0;

        Lector(String s) {
            this.s = s;
        }

        JsonException error(String mensaje) {
            return new JsonException(mensaje + " (posicion " + i + ")");
        }

        void espacios() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    i++;
                } else {
                    break;
                }
            }
        }

        Object valor(int nivel) {
            if (nivel > PROFUNDIDAD_MAXIMA) {
                throw error("JSON demasiado anidado");
            }
            espacios();
            if (i >= s.length()) {
                throw error("Se esperaba un valor");
            }
            char c = s.charAt(i);
            if (c == '{') {
                return objeto(nivel);
            } else if (c == '[') {
                return arreglo(nivel);
            } else if (c == '"') {
                return cadena();
            } else if (c == 't') {
                return literal("true", Boolean.TRUE);
            } else if (c == 'f') {
                return literal("false", Boolean.FALSE);
            } else if (c == 'n') {
                return literal("null", null);
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                return numero();
            }
            throw error("Caracter inesperado '" + c + "'");
        }

        private Object literal(String texto, Object valor) {
            if (!s.startsWith(texto, i)) {
                throw error("Valor invalido");
            }
            i += texto.length();
            return valor;
        }

        private Map<String, Object> objeto(int nivel) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            i++; // {
            espacios();
            if (i < s.length() && s.charAt(i) == '}') {
                i++;
                return mapa;
            }
            while (true) {
                espacios();
                if (i >= s.length() || s.charAt(i) != '"') {
                    throw error("Se esperaba el nombre de un campo entre comillas");
                }
                String clave = cadena();
                espacios();
                if (i >= s.length() || s.charAt(i) != ':') {
                    throw error("Se esperaba ':'");
                }
                i++;
                mapa.put(clave, valor(nivel + 1));
                espacios();
                if (i >= s.length()) {
                    throw error("Objeto sin cerrar");
                }
                char c = s.charAt(i++);
                if (c == '}') {
                    return mapa;
                }
                if (c != ',') {
                    i--;
                    throw error("Se esperaba ',' o '}'");
                }
            }
        }

        private List<Object> arreglo(int nivel) {
            List<Object> lista = new ArrayList<>();
            i++; // [
            espacios();
            if (i < s.length() && s.charAt(i) == ']') {
                i++;
                return lista;
            }
            while (true) {
                lista.add(valor(nivel + 1));
                espacios();
                if (i >= s.length()) {
                    throw error("Arreglo sin cerrar");
                }
                char c = s.charAt(i++);
                if (c == ']') {
                    return lista;
                }
                if (c != ',') {
                    i--;
                    throw error("Se esperaba ',' o ']'");
                }
            }
        }

        private String cadena() {
            i++; // comilla de apertura
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) {
                    throw error("Texto sin cerrar");
                }
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c < 0x20) {
                    i--;
                    throw error("Caracter de control sin escapar dentro de un texto");
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (i >= s.length()) {
                    throw error("Escape incompleto");
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 > s.length()) {
                            throw error("Escape \\u incompleto");
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("Escape \\u invalido");
                        }
                        i += 4;
                    }
                    default -> {
                        i--;
                        throw error("Escape invalido '\\" + e + "'");
                    }
                }
            }
        }

        private Object numero() {
            int inicio = i;
            if (s.charAt(i) == '-') {
                i++;
            }
            int digitosEnteros = digitos();
            if (digitosEnteros == 0) {
                throw error("Numero invalido");
            }
            boolean decimal = false;
            if (i < s.length() && s.charAt(i) == '.') {
                decimal = true;
                i++;
                if (digitos() == 0) {
                    throw error("Numero invalido: faltan digitos despues del punto");
                }
            }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                decimal = true;
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                    i++;
                }
                if (digitos() == 0) {
                    throw error("Numero invalido: exponente sin digitos");
                }
            }
            String texto = s.substring(inicio, i);
            if (!decimal) {
                try {
                    return Long.valueOf(texto);
                } catch (NumberFormatException e) {
                    // fuera del rango de long: se conserva como Double
                }
            }
            return Double.valueOf(texto);
        }

        private int digitos() {
            int cuenta = 0;
            while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
                cuenta++;
            }
            return cuenta;
        }
    }
}
