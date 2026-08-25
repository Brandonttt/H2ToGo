package com.h2togo.backend.security;

import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticación por token opaco en BD (RN-018). Lee {@code Authorization: Bearer <token>},
 * busca el usuario por {@code token_sesion}, valida expiración y {@code cuenta_activa}, y
 * monta {@link UsuarioPrincipal} en el contexto.
 * <p>Expiración por inactividad (RNF-004, §10 #15): si al validar quedan menos de
 * {@code token-refresco-dias} días de vigencia, extiende la expiración a now()+{@code token-dias}
 * (una sola escritura), de modo que un usuario activo no caduca y uno inactivo &gt;30 días sí.
 */
@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final UsuarioRepository usuarioRepository;
    private final int tokenDias;
    private final int tokenRefrescoDias;

    public TokenAuthFilter(UsuarioRepository usuarioRepository,
            @Value("${h2togo.auth.token-dias}") int tokenDias,
            @Value("${h2togo.auth.token-refresco-dias}") int tokenRefrescoDias) {
        this.usuarioRepository = usuarioRepository;
        this.tokenDias = tokenDias;
        this.tokenRefrescoDias = tokenRefrescoDias;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<Usuario> encontrado = usuarioRepository.findByTokenSesion(token);
            if (encontrado.isPresent() && esVigente(encontrado.get())) {
                autenticar(encontrado.get());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIJO)) {
            String t = header.substring(PREFIJO.length()).trim();
            return t.isEmpty() ? null : t;
        }
        return null;
    }

    private boolean esVigente(Usuario usuario) {
        if (!usuario.isCuentaActiva()) {
            return false;
        }
        OffsetDateTime expira = usuario.getSesionFechaExpiracion();
        return expira != null && expira.isAfter(OffsetDateTime.now());
    }

    private void autenticar(Usuario usuario) {
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario.getId(), usuario.getRol());
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(principal.authority())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        refrescarSiHaceFalta(usuario);
    }

    /** RNF-004 deslizante (§10 #15): renueva solo cuando queda poca vigencia. */
    private void refrescarSiHaceFalta(Usuario usuario) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (usuario.getSesionFechaExpiracion().isBefore(ahora.plusDays(tokenRefrescoDias))) {
            usuario.setSesionFechaExpiracion(ahora.plusDays(tokenDias));
            usuarioRepository.save(usuario);
        }
    }
}
