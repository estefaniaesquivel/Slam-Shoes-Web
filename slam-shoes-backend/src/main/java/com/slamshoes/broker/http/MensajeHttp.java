package com.slamshoes.broker.http;

/**
 * Respuesta HTTP ya leída por {@link TransporteHttp}: código, tipo de contenido y cuerpo en texto.
 *
 * REFACTOR: Introduce Parameter Object (aquí aplicado al valor de retorno)
 * RAZÓN: con sockets el "resultado" de hablar con otro proceso era un String suelto (una línea JSON).
 * HTTP trae tres datos que importan (código, tipo y cuerpo); agruparlos en un objeto evita pasar
 * tres valores sueltos de un método a otro y permite decidir reintentos según el código.
 *
 * @param codigo        código HTTP (200, 404, 503...)
 * @param tipoContenido valor de la cabecera Content-Type, o null si no vino
 * @param cuerpo        cuerpo de la respuesta (nunca null; vacío si no hubo)
 */
public record MensajeHttp(int codigo, String tipoContenido, String cuerpo) {

    public MensajeHttp {
        cuerpo = cuerpo == null ? "" : cuerpo;
    }

    /** true para códigos 2xx. */
    public boolean esCorrecto() {
        return codigo >= 200 && codigo < 300;
    }

    /**
     * true si conviene reintentar: el proveedor aún está arrancando o saturado (502, 503, 504, 429).
     * En Render un servicio "dormido" responde 502/503 mientras despierta.
     */
    public boolean esTransitorio() {
        return codigo == 502 || codigo == 503 || codigo == 504 || codigo == 429;
    }
}
