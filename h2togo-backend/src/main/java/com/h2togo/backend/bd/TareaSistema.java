package com.h2togo.backend.bd;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Trabajo del backend que no es del usuario de la petición aunque ocurra durante ella: avisar a
 * los repartidores de un pedido nuevo o leer el token FCM de quien recibe una notificación. Corre
 * en una transacción nueva (otra conexión) con el rol {@code h2togo_sistema}.
 */
@Component
public class TareaSistema {

    private static final ThreadLocal<Boolean> ACTIVA = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final TransactionTemplate transaccionNueva;

    public TareaSistema(PlatformTransactionManager transacciones) {
        this.transaccionNueva = new TransactionTemplate(transacciones);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    static boolean activa() {
        return ACTIVA.get();
    }

    public <T> T consultar(Supplier<T> trabajo) {
        boolean previa = ACTIVA.get();
        ACTIVA.set(Boolean.TRUE);
        try {
            return transaccionNueva.execute(estado -> trabajo.get());
        } finally {
            ACTIVA.set(previa);
        }
    }

    public void ejecutar(Runnable trabajo) {
        consultar(() -> {
            trabajo.run();
            return null;
        });
    }

    /** Como {@link #ejecutar}, pero cuando confirme la transacción actual (sin transacción, ya). */
    public void alConfirmar(Runnable trabajo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ejecutar(trabajo);
                }
            });
        } else {
            ejecutar(trabajo);
        }
    }
}
