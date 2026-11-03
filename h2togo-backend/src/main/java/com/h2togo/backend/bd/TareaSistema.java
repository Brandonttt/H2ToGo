package com.h2togo.backend.bd;

import jakarta.annotation.PreDestroy;
import java.util.function.Supplier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
 *
 * <p>Lo que se hace al confirmar ({@link #alConfirmar}, {@link #despuesDelCommit}) corre en
 * segundo plano: {@code afterCommit} se ejecuta cuando la petición todavía tiene su conexión, y
 * pedir otra ahí agota el pool con 10 peticiones simultáneas (cada una con una conexión, esperando
 * la segunda). En segundo plano, la petición suelta la suya antes de que se pida la nueva.
 */
@Component
public class TareaSistema {

    private static final ThreadLocal<Boolean> ACTIVA = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final TransactionTemplate transaccionNueva;
    private final ThreadPoolTaskExecutor segundoPlano;

    public TareaSistema(PlatformTransactionManager transacciones) {
        this.transaccionNueva = new TransactionTemplate(transacciones);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.segundoPlano = new ThreadPoolTaskExecutor();
        this.segundoPlano.setThreadNamePrefix("tarea-sistema-");
        this.segundoPlano.setCorePoolSize(2);
        this.segundoPlano.setMaxPoolSize(2);
        this.segundoPlano.setWaitForTasksToCompleteOnShutdown(true);
        this.segundoPlano.initialize();
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

    /** Como {@link #ejecutar}, pero en segundo plano cuando confirme la transacción actual. */
    public void alConfirmar(Runnable trabajo) {
        despuesDelCommit(() -> ejecutar(trabajo));
    }

    /**
     * Corre la acción en segundo plano cuando confirme la transacción actual (sin transacción,
     * ya). La acción no abre transacción: si toca la base, que use {@link #ejecutar}.
     */
    public void despuesDelCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    segundoPlano.execute(accion);
                }
            });
        } else {
            segundoPlano.execute(accion);
        }
    }

    @PreDestroy
    void detener() {
        segundoPlano.shutdown();
    }
}
