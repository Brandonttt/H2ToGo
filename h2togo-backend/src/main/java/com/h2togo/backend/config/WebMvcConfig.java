package com.h2togo.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enrutado de la consola de pruebas (F13). El manejador de recursos estáticos no resuelve un
 * directorio ({@code /console/}) a su {@code index.html}, así que se reenvían las rutas base a
 * la página, para que la consola abra tanto en {@code /console} como en {@code /console/}.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/console/index.html");
        registry.addViewController("/console").setViewName("forward:/console/index.html");
        registry.addViewController("/console/").setViewName("forward:/console/index.html");
    }
}
