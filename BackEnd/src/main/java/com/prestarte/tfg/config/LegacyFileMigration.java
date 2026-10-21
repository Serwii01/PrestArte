package com.prestarte.tfg.config;

import com.prestarte.tfg.repository.DBFileRepository;
import com.prestarte.tfg.service.FileBlobStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Mueve a disco el contenido de los archivos subidos antes de que
 * existiera {@link FileBlobStore}. Se activa con
 * {@code app.storage.migrate-legacy=true}; cada archivo va en su propia
 * transacción, así que se puede interrumpir y relanzar sin problema.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.migrate-legacy", havingValue = "true")
public class LegacyFileMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyFileMigration.class);

    private final DBFileRepository dbFileRepository;
    private final FileBlobStore store;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> ids = dbFileRepository.findLegacyIds();
        log.info("Migrando a disco {} archivos guardados en la base de datos", ids.size());
        int moved = 0;
        for (String id : ids) {
            try {
                transactionTemplate.executeWithoutResult(status -> dbFileRepository.findById(id).ifPresent(f -> {
                    f.setStorageKey(store.write(f.getData()));
                    f.setData(null);
                }));
                moved++;
            } catch (RuntimeException e) {
                log.warn("No se pudo migrar el archivo {}: {}", id, e.getMessage());
            }
        }
        log.info("Migración de archivos terminada: {} de {}", moved, ids.size());
    }
}
