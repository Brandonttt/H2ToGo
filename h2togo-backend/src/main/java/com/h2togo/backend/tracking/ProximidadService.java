package com.h2togo.backend.tracking;

import com.h2togo.backend.notificaciones.PushService;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Aviso de proximidad a 500 m (RF-005/RN-010). El flag anti-duplicado es en memoria
 * (suficiente para una instancia; §10 #2). Una sola notificación por pedido.
 */
@Service
public class ProximidadService {

    private final PushService pushService;
    private final ConcurrentHashMap<Integer, Boolean> notificados = new ConcurrentHashMap<>();

    public ProximidadService(PushService pushService) {
        this.pushService = pushService;
    }

    /** Notifica al cliente que su pedido está cerca, solo la primera vez por pedido. */
    public void avisarProximidad(int idPedido, int idCliente) {
        if (notificados.putIfAbsent(idPedido, Boolean.TRUE) == null) {
            pushService.notificar(idCliente, "Tu pedido está cerca",
                    "El repartidor está a menos de 500 m de tu domicilio.");
        }
    }

    /** Limpia el flag (p. ej. al cerrar el pedido). */
    public void olvidar(int idPedido) {
        notificados.remove(idPedido);
    }
}
