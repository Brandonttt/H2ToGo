package com.h2togo.backend.tracking;

import com.h2togo.backend.notificaciones.PushService;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Aviso de proximidad a 500 m (RF-005/RN-010) y de llegada del repartidor al domicilio.
 * Una vez iniciado el contador de 10 minutos de espera tras la llegada, reenvía un recordatorio
 * al cliente cada 3 minutos (a los 3, 6 y 9 minutos) mientras el pedido siga activo.
 */
@Service
public class ProximidadService {

    private static final class RegistroLlegada {
        final int idCliente;
        final OffsetDateTime llegoEn;
        volatile int ultimoEscalonRecordado;

        RegistroLlegada(int idCliente, OffsetDateTime llegoEn) {
            this.idCliente = idCliente;
            this.llegoEn = llegoEn;
            this.ultimoEscalonRecordado = 0;
        }
    }

    private final PushService pushService;
    private final ConcurrentHashMap<Integer, Boolean> notificados = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, RegistroLlegada> llegadas = new ConcurrentHashMap<>();

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
     * Registra que el repartidor llegó al domicilio y avisa al cliente al iniciar el contador de
     * 10 minutos. Si ya estaba registrado, verifica si corresponde emitir un recordatorio de 3 min.
     */
    public OffsetDateTime avisarLlegada(int idPedido, int idCliente) {
        OffsetDateTime ahora = OffsetDateTime.now();
        RegistroLlegada nuevo = new RegistroLlegada(idCliente, ahora);
        RegistroLlegada previo = llegadas.putIfAbsent(idPedido, nuevo);
        if (previo != null) {
            verificarRecordatorio(idPedido, previo, ahora);
            return previo.llegoEn;
        }
        pushService.notificar(idCliente, "Tu repartidor llegó",
                "El repartidor ya está afuera de tu domicilio. Sal a recibir tu pedido.",
                Map.of("tipo", "repartidor_llego", "idPedido", String.valueOf(idPedido)));
        return ahora;
    }

    /** Revisa periódicamente los pedidos en espera para notificar al cliente cada 3 minutos (3, 6 y 9 min). */
    @Scheduled(fixedDelay = 15_000)
    public void enviarRecordatoriosEspera() {
        if (llegadas.isEmpty()) {
            return;
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        for (Map.Entry<Integer, RegistroLlegada> entry : llegadas.entrySet()) {
            Integer idPedido = entry.getKey();
            RegistroLlegada reg = entry.getValue();
            long segs = Duration.between(reg.llegoEn, ahora).getSeconds();
            if (segs >= 660) {
                llegadas.remove(idPedido);
                continue;
            }
            verificarRecordatorio(idPedido, reg, ahora);
        }
    }

    private void verificarRecordatorio(int idPedido, RegistroLlegada reg, OffsetDateTime ahora) {
        long segs = Duration.between(reg.llegoEn, ahora).getSeconds();
        if (segs < 180 || segs >= 600) {
            return;
        }
        int escalon = (int) (segs / 180); // 1 (3 min), 2 (6 min), 3 (9 min)
        if (escalon >= 1 && escalon <= 3 && escalon > reg.ultimoEscalonRecordado) {
            synchronized (reg) {
                if (escalon > reg.ultimoEscalonRecordado) {
                    reg.ultimoEscalonRecordado = escalon;
                    int minTranscurridos = escalon * 3;
                    int minRestantes = Math.max(1, 10 - minTranscurridos);
                    pushService.notificar(reg.idCliente, "Tu repartidor te espera afuera",
                            "El repartidor lleva " + minTranscurridos + " min esperando afuera de tu domicilio (quedan "
                                    + minRestantes + " min). Sal a recibir tu pedido.",
                            Map.of(
                                    "tipo", "repartidor_llego",
                                    "idPedido", String.valueOf(idPedido),
                                    "recordatorio", String.valueOf(escalon)));
                }
            }
        }
    }

    /** Hora en que el repartidor llegó al domicilio, si ya llegó. */
    public Optional<OffsetDateTime> llegada(int idPedido) {
        return Optional.ofNullable(llegadas.get(idPedido)).map(r -> r.llegoEn);
    }

    /** Limpia los flags (p. ej. al cerrar el pedido). */
    public void olvidar(int idPedido) {
        notificados.remove(idPedido);
        llegadas.remove(idPedido);
    }
}
