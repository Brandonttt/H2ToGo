package com.h2togo.backend.tracking;

import com.h2togo.backend.notificaciones.PushService;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Aviso de proximidad a 500 m (RF-005/RN-010) y de llegada del repartidor al domicilio. Los
 * flags anti-duplicado son en memoria (suficiente para una instancia; §10 #2). Una sola
 * notificación de cada tipo por pedido.
 */
@Service
public class ProximidadService {

    private final PushService pushService;
    private final ConcurrentHashMap<Integer, Boolean> notificados = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, OffsetDateTime> llegadas = new ConcurrentHashMap<>();

    public ProximidadService(PushService pushService) {
        this.pushService = pushService;
    }

    /** Notifica al cliente que su pedido está cerca, solo la primera vez por pedido. */
    public void avisarProximidad(int idPedido, int idCliente) {
        if (notificados.putIfAbsent(idPedido, Boolean.TRUE) == null) {
            pushService.notificar(idCliente, "Tu pedido está cerca",
                    "El repartidor está a menos de 500 m de tu domicilio.",
                    Map.of("tipo", "pedido_cerca", "idPedido", String.valueOf(idPedido)));
        }
    }

    /**
     * Registra que el repartidor llegó al domicilio y avisa al cliente, solo la primera vez por
     * pedido. Devuelve la hora de llegada (la primera, si ya se había registrado).
     */
    public OffsetDateTime avisarLlegada(int idPedido, int idCliente) {
        OffsetDateTime ahora = OffsetDateTime.now();
        OffsetDateTime previa = llegadas.putIfAbsent(idPedido, ahora);
        if (previa != null) {
            return previa;
        }
        pushService.notificar(idCliente, "Tu repartidor llegó",
                "El repartidor ya está afuera de tu domicilio. Sal a recibir tu pedido.",
                Map.of("tipo", "repartidor_llego", "idPedido", String.valueOf(idPedido)));
        return ahora;
    }

    /** Hora en que el repartidor llegó al domicilio, si ya llegó. */
    public Optional<OffsetDateTime> llegada(int idPedido) {
        return Optional.ofNullable(llegadas.get(idPedido));
    }

    /** Limpia los flags (p. ej. al cerrar el pedido). */
    public void olvidar(int idPedido) {
        notificados.remove(idPedido);
        llegadas.remove(idPedido);
    }
}
