package com.h2togo.backend.config;

import com.h2togo.backend.security.RestAccessDeniedHandler;
import com.h2togo.backend.security.RestAuthenticationEntryPoint;
import com.h2togo.backend.security.TokenAuthFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración de seguridad de F2. Autenticación por token opaco en BD (RN-018) vía
 * {@link TokenAuthFilter}; sesión sin estado; BCrypt para contraseñas (RN-017/RNF-005).
 * Público: {@code /auth/**}, {@code /health}, swagger y el handshake WS; el resto exige
 * autenticación y {@code /admin/**} exige rol ADMIN (RNF-008).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
            TokenAuthFilter tokenAuthFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .cors(c -> c.configurationSource(cors))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/health",
                                "/ws/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                // Consola de pruebas (F13): la página estática es pública;
                                // las llamadas a la API que hace siguen exigiendo token/rol.
                                "/",
                                "/console",
                                "/console/**")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/clientes/**").hasRole("CLIENTE")
                        .requestMatchers("/api/v1/negocios/me/**").hasRole("REPARTIDOR")
                        .requestMatchers("/api/v1/inventario/**").hasRole("REPARTIDOR")
                        .requestMatchers("/api/v1/repartidores/**").hasRole("REPARTIDOR")
                        .requestMatchers("/api/v1/solicitudes/**").hasRole("REPARTIDOR")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
