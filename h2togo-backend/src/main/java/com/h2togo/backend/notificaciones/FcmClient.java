package com.h2togo.backend.notificaciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Envío por Firebase Cloud Messaging (API HTTP v1) con una cuenta de servicio. Queda inactivo si
 * {@code h2togo.fcm.credenciales-json} está vacío (desarrollo): entonces solo opera el WebSocket.
 * Acepta el JSON tal cual o codificado en base64 (más cómodo como secreto de Azure).
 * Un token inválido se borra del usuario; un error del servidor de FCM se lanza para reintentar.
 */
@Component
public class FcmClient {

    private static final Logger log = LoggerFactory.getLogger(FcmClient.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final NamedParameterJdbcTemplate jdbc;
    private final RestClient http = RestClient.create();
    private final GoogleCredentials credenciales;
    private final String urlEnvio;

    public FcmClient(NamedParameterJdbcTemplate jdbc,
            @Value("${h2togo.fcm.credenciales-json:}") String credencialesJson) {
        this.jdbc = jdbc;
        if (credencialesJson == null || credencialesJson.isBlank()) {
            this.credenciales = null;
            this.urlEnvio = null;
            log.info("FCM deshabilitado (sin h2togo.fcm.credenciales-json): las notificaciones solo van por WebSocket.");
            return;
        }
        try {
            String texto = credencialesJson.trim();
            byte[] bytes = texto.startsWith("{")
                    ? texto.getBytes(StandardCharsets.UTF_8)
                    : Base64.getDecoder().decode(texto);
            String proyecto = new ObjectMapper().readTree(bytes).path("project_id").asText();
            this.credenciales = GoogleCredentials.fromStream(new ByteArrayInputStream(bytes)).createScoped(List.of(SCOPE));
            this.urlEnvio = "https://fcm.googleapis.com/v1/projects/" + proyecto + "/messages:send";
            log.info("FCM habilitado para el proyecto {}", proyecto);
        } catch (IOException e) {
            throw new IllegalStateException("h2togo.fcm.credenciales-json no es una cuenta de servicio válida", e);
        }
    }

    public boolean habilitado() {
        return credenciales != null;
    }

    public void enviar(Notificacion n) {
        if (!habilitado()) {
            return;
        }
        String token = jdbc.query(
                "SELECT token_fcm FROM usuarios WHERE id_usuario = :id AND cuenta_activa",
                new MapSqlParameterSource("id", n.idUsuario()), rs -> rs.next() ? rs.getString(1) : null);
        if (token == null || token.isBlank()) {
            return; // sin dispositivo registrado (o sesión cerrada): solo WebSocket
        }

        Map<String, String> datos = new HashMap<>(n.datos());
        datos.put("idNotificacion", n.id());
        Map<String, Object> mensaje = Map.of("message", Map.of(
                "token", token,
                "notification", Map.of("title", n.titulo(), "body", n.mensaje()),
                "data", datos,
                "android", Map.of("priority", "high", "notification", Map.of("channel_id", "pedidos"))));
        try {
            http.post().uri(urlEnvio)
                    .header("Authorization", "Bearer " + tokenAcceso())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mensaje)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (tokenInvalido(e.getStatusCode(), e.getResponseBodyAsString())) {
                log.info("Token FCM inválido del usuario {}; se elimina.", n.idUsuario());
                jdbc.update("UPDATE usuarios SET token_fcm = NULL WHERE id_usuario = :id AND token_fcm = :t",
                        new MapSqlParameterSource().addValue("id", n.idUsuario()).addValue("t", token));
                return;
            }
            throw e; // 429 / 5xx: que RabbitMQ reintente
        }
    }

    private String tokenAcceso() {
        try {
            credenciales.refreshIfExpired();
            return credenciales.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo obtener el token de acceso de FCM", e);
        }
    }

    /** 404 UNREGISTERED o 400 INVALID_ARGUMENT sobre el token: el dispositivo ya no existe. */
    private static boolean tokenInvalido(HttpStatusCode status, String cuerpo) {
        return status.value() == 404 || (status.value() == 400 && cuerpo != null && cuerpo.contains("INVALID_ARGUMENT"));
    }
}
