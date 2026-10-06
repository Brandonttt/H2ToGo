package com.h2togo.backend.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Token FCM del dispositivo con sesión activa (Firebase puede rotarlo en cualquier momento). */
public record DispositivoRequest(@NotBlank @Size(max = 255) String tokenFcm) {
}
