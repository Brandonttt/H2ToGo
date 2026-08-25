package com.h2togo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend H2ToGo (Trabajo Terminal 2026-B176).
 * Empaqueta el Tomcat embebido; se ejecuta con {@code mvn spring-boot:run} en
 * desarrollo o {@code java -jar} en despliegue.
 */
@SpringBootApplication
public class H2ToGoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(H2ToGoBackendApplication.class, args);
    }
}
