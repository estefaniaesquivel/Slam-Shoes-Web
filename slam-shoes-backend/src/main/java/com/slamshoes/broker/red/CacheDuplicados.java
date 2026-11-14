package com.slamshoes.broker.red;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Evita ejecutar dos veces la misma solicitud (mismo idSolicitud).
 *
 * Caso típico: el cliente no recibe la respuesta a tiempo y reintenta con el MISMO id. En vez de
 * crear dos pedidos, la segunda llamada recibe la respuesta de la primera. Si la primera todavía
 * se está ejecutando, la segunda ESPERA ese resultado (no se ejecuta en paralelo).
 *
 * Solo se recuerdan las respuestas que el llamador marque como guardables (las exitosas); un error
 * se vuelve a intentar normalmente. Guarda un máximo de entradas y descarta las más viejas.
 */
public final class CacheDuplicados<V> {

    /** Valor obtenido y si vino de una ejecución anterior (duplicada). */
    public static final class Resultado<V> {
        public final V valor;
        public final boolean duplicada;

        Resultado(V valor, boolean duplicada) {
            this.valor = valor;
            this.duplicada = duplicada;
        }
    }

    private final int maximo;
    private final ConcurrentHashMap<String, FutureTask<V>> mapa = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> orden = new ConcurrentLinkedQueue<>();

    public CacheDuplicados(int maximo) {
        this.maximo = Math.max(1, maximo);
    }

    /**
     * Ejecuta la tarea para este id, o devuelve/espera el resultado de otra ejecución con el mismo id.
     *
     * @param guardable decide si el resultado se recuerda para futuros duplicados
     */
    public Resultado<V> ejecutar(String id, Supplier<V> tarea, Predicate<V> guardable) {
        FutureTask<V> nueva = new FutureTask<>(tarea::get);
        FutureTask<V> previa = mapa.putIfAbsent(id, nueva);
        boolean duplicada = previa != null;
        FutureTask<V> usada = duplicada ? previa : nueva;

        if (!duplicada) {
            orden.add(id);
            nueva.run();
        }

        V valor;
        try {
            valor = usada.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!duplicada) {
                mapa.remove(id, nueva);
            }
            throw new IllegalStateException("Interrumpido mientras se esperaba el resultado", e);
        } catch (ExecutionException e) {
            if (!duplicada) {
                mapa.remove(id, nueva);
            }
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            if (causa instanceof Error err) {
                throw err;
            }
            throw new IllegalStateException(causa);
        }

        if (!duplicada) {
            if (!guardable.test(valor)) {
                mapa.remove(id, nueva);
            }
            recortar();
        }
        return new Resultado<>(valor, duplicada);
    }

    public int tamano() {
        return mapa.size();
    }

    private void recortar() {
        while (mapa.size() > maximo) {
            String viejo = orden.poll();
            if (viejo == null) {
                break;
            }
            mapa.remove(viejo);
        }
    }
}
