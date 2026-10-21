package com.prestarte.tfg.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda el contenido de los archivos en disco, fuera de la base de datos.
 *
 * La base de datos solo guarda los metadatos ({@code DBFile}) y una clave
 * aleatoria; los bytes van a {@code app.storage.dir}, repartidos en
 * subcarpetas por los dos primeros caracteres de la clave para no tener
 * miles de ficheros en un mismo directorio. Así las copias de seguridad
 * de MySQL son pequeñas y los archivos se pueden respaldar (o mover a un
 * almacenamiento de objetos) por separado.
 */
@Component
public class FileBlobStore {

    /** Las claves las genera esta clase: nunca rutas que venga del usuario. */
    private static final Pattern KEY = Pattern.compile("[0-9a-f]{32}");

    private final Path root;

    public FileBlobStore(@Value("${app.storage.dir:./data/files}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    /** Escribe el contenido y devuelve su clave. */
    public String write(byte[] data) {
        String key = UUID.randomUUID().toString().replace("-", "");
        Path target = pathOf(key);
        try {
            Files.createDirectories(target.getParent());
            // Primero a un temporal y luego se mueve: nunca queda un fichero a medias.
            Path tmp = Files.createTempFile(target.getParent(), key, ".tmp");
            Files.write(tmp, data);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return key;
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(pathOf(key));
        } catch (IOException e) {
            throw new UncheckedIOException("No se encuentra el contenido del archivo", e);
        }
    }

    /** Borra el contenido; si ya no existe, no hace nada. */
    public void delete(String key) {
        try {
            Files.deleteIfExists(pathOf(key));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar el archivo", e);
        }
    }

    private Path pathOf(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Clave de archivo no válida");
        }
        return root.resolve(key.substring(0, 2)).resolve(key);
    }
}
