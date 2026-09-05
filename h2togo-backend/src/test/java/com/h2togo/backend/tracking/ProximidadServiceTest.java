package com.h2togo.backend.tracking;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.h2togo.backend.notificaciones.PushService;
import org.junit.jupiter.api.Test;

/** El aviso de proximidad se envía una sola vez por pedido (dedup en memoria, §10 #2). */
class ProximidadServiceTest {

    @Test
    void avisaUnaSolaVezPorPedido() {
        PushService push = mock(PushService.class);
        ProximidadService service = new ProximidadService(push);

        service.avisarProximidad(10, 99);
        service.avisarProximidad(10, 99); // segunda vez: no debe notificar de nuevo

        verify(push, times(1)).notificar(eq(99), eq("Tu pedido está cerca"), anyString());
    }

    @Test
    void trasOlvidarVuelveAAvisar() {
        PushService push = mock(PushService.class);
        ProximidadService service = new ProximidadService(push);

        service.avisarProximidad(10, 99);
        service.olvidar(10);
        service.avisarProximidad(10, 99);

        verify(push, times(2)).notificar(eq(99), anyString(), anyString());
    }
}
