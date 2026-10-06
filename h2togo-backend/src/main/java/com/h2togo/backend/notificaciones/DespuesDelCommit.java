package com.h2togo.backend.notificaciones;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Ejecuta una acción cuando la transacción actual confirma; sin transacción, de inmediato. */
final class DespuesDelCommit {

    private DespuesDelCommit() {
    }

    static void ejecutar(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
