package com.h2togo.backend.bd;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * [S1] Envuelve el DataSource para que cada conexión adopte el rol de PostgreSQL del actor. Con
 * {@code h2togo.bd.rol-por-actor=false} el backend usa directamente el usuario de login (solo
 * para una BD sin los roles h2togo_*).
 */
@Configuration
public class BaseDatosConfig {

    @Bean
    static BeanPostProcessor rolPorActor(Environment entorno) {
        boolean activo = entorno.getProperty("h2togo.bd.rol-por-actor", Boolean.class, true);
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                return activo && bean instanceof DataSource ds && !(bean instanceof RolPorActorDataSource)
                        ? new RolPorActorDataSource(ds) : bean;
            }
        };
    }
}
