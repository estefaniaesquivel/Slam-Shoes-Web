package com.slamshoes.config;

import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.slamshoes.broker.ProveedorServicio;
import com.slamshoes.broker.http.ExposicionProveedores;
import com.slamshoes.broker.http.NucleoBroker;

/**
 * Crea los dos componentes del Broker HTTP según el rol que tenga esta instancia en el despliegue:
 * <ul>
 *   <li>{@code broker.servidor.activo} (BROKER_SERVIDOR_ACTIVO): esta instancia ES el Broker (directorio + proxy).</li>
 *   <li>{@code broker.proveedores.activo} (BROKER_PROVEEDORES_ACTIVO): esta instancia aloja proveedores
 *       (productos, usuarios, pedidos) y se registra en el Broker.</li>
 * </ul>
 * Por omisión ambos están activos (todo en una sola aplicación, como en desarrollo). En Render se pueden desplegar
 * instancias separadas apagando uno de los dos roles.
 *
 * REFACTOR: Move Function (el armado de componentes sale de BrokerArranque)
 * RAZÓN: BrokerArranque creaba a mano servidores, pools y puertos. Crear los objetos es un asunto de configuración y
 * arrancar/detener es otro; separarlos deja a Spring construir los objetos con sus propiedades y a BrokerArranque
 * solo anunciar los proveedores.
 */
@Configuration
public class BrokerHttpConfig {

    @Bean
    @ConditionalOnProperty(name = "broker.servidor.activo", havingValue = "true", matchIfMissing = true)
    public NucleoBroker nucleoBroker(@Value("${broker.bitacora:bitacora-broker.log}") String archivoBitacora,
                                     @Value("${broker.timeout-ms:20000}") int timeoutMs,
                                     @Value("${broker.reintentos:1}") int reintentos,
                                     @Value("${broker.max-fallos:3}") int maximoFallos,
                                     @Value("${broker.token:}") String token) {
        return new NucleoBroker(Paths.get(archivoBitacora), 2000, timeoutMs, reintentos, maximoFallos, token);
    }

    @Bean
    @ConditionalOnProperty(name = "broker.proveedores.activo", havingValue = "true", matchIfMissing = true)
    public ExposicionProveedores exposicionProveedores(List<ProveedorServicio> proveedores,
                                                        @Value("${broker.token:}") String token) {
        return new ExposicionProveedores(proveedores, token);
    }
}
