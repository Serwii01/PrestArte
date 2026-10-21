package com.prestarte.tfg.model.entity;

import com.prestarte.tfg.service.FileBlobStore;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PrePersist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Saca a disco el contenido de cada {@link DBFile} al guardarlo, de forma
 * transparente para quien lo crea (que sigue rellenando {@code data}).
 *
 * Hibernate obtiene este listener del contexto de Spring, por eso puede
 * recibir el {@link FileBlobStore} inyectado.
 */
@Component
public class DBFileStorageListener {

    private static final Logger log = LoggerFactory.getLogger(DBFileStorageListener.class);

    private final FileBlobStore store;

    public DBFileStorageListener(FileBlobStore store) {
        this.store = store;
    }

    @PrePersist
    public void moveToDisk(DBFile file) {
        if (file.getData() == null || file.getStorageKey() != null) return;
        String key = store.write(file.getData());
        file.setStorageKey(key);
        file.setData(null);
        // Si la transacción falla, el fichero escrito no lo referencia nadie: se borra.
        afterRollback(() -> store.delete(key));
    }

    @PostRemove
    public void deleteFromDisk(DBFile file) {
        String key = file.getStorageKey();
        if (key == null) return;
        // Solo cuando el borrado en la base de datos es definitivo.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeDelete(key);
                }
            });
        } else {
            safeDelete(key);
        }
    }

    private void afterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) action.run();
            }
        });
    }

    private void safeDelete(String key) {
        try {
            store.delete(key);
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar del disco el archivo {}: {}", key, e.getMessage());
        }
    }
}
