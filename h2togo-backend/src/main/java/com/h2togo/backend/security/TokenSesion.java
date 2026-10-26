package com.h2togo.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * El token opaco de sesión se guarda como su SHA-256 (v7 [C4]): si se filtra un respaldo de la
 * BD, los tokens no sirven para entrar. El token en claro solo lo tiene el dispositivo. No hace
 * falta sal ni un hash lento como en las contraseñas: el token son 256 bits aleatorios.
 */
public final class TokenSesion {

    private TokenSesion() {
    }

    /** SHA-256 en hexadecimal; igual a {@code encode(sha256(convert_to(token, 'UTF8')), 'hex')}. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
