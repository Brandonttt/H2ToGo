package com.h2togo.backend.tracking;

import com.h2togo.backend.pedidos.dto.UbicacionRequest;
import com.h2togo.backend.security.StompPrincipal;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/**
 * Canal STOMP de rastreo (CU-006). El repartidor asignado publica su ubicación en
 * {@code /app/pedidos/{id}/ubicacion}; el backend persiste, evalúa proximidad y la reenvía a
 * {@code /topic/pedidos/{id}/ubicacion}. Otro usuario no puede publicar en ese pedido (RN-016).
 */
@Controller
public class TrackingController {

    private final RastreoService rastreoService;
    private final AccesoRastreo acceso;

    public TrackingController(RastreoService rastreoService, AccesoRastreo acceso) {
        this.rastreoService = rastreoService;
        this.acceso = acceso;
    }

    @MessageMapping("/pedidos/{id}/ubicacion")
    public void ubicacion(@DestinationVariable int id, @Payload UbicacionRequest req, Principal principal) {
        int idUsuario = ((StompPrincipal) principal).idUsuario();
        if (!acceso.puedePublicar(idUsuario, id)) {
            throw new AccessDeniedException("Solo el repartidor asignado puede publicar la ubicación de este pedido.");
        }
        rastreoService.publicar(idUsuario, req.lat(), req.lon());
    }
}
