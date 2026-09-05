package com.h2togo.backend.tracking;

import com.h2togo.backend.pedidos.dto.UbicacionRequest;
import com.h2togo.backend.security.StompPrincipal;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Canal STOMP de rastreo (CU-006). El repartidor publica su ubicación en
 * {@code /app/pedidos/{id}/ubicacion}; el backend persiste, evalúa proximidad y rebota la
 * posición a {@code /topic/pedidos/{id}/ubicacion} para el cliente suscrito.
 */
@Controller
public class TrackingController {

    private final UbicacionService ubicacionService;
    private final SimpMessagingTemplate messaging;

    public TrackingController(UbicacionService ubicacionService, SimpMessagingTemplate messaging) {
        this.ubicacionService = ubicacionService;
        this.messaging = messaging;
    }

    @MessageMapping("/pedidos/{id}/ubicacion")
    public void ubicacion(@DestinationVariable int id, @Payload UbicacionRequest req, Principal principal) {
        int idRepartidor = ((StompPrincipal) principal).idUsuario();
        ubicacionService.reportar(idRepartidor, req.lat(), req.lon());
        Object payload = Map.of("lat", req.lat(), "lon", req.lon(),
                "timestamp", OffsetDateTime.now().toString());
        messaging.convertAndSend("/topic/pedidos/" + id + "/ubicacion", payload);
    }
}
