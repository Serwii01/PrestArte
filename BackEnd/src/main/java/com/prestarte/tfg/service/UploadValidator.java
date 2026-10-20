package com.prestarte.tfg.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

/**
 * Validación centralizada de los archivos subidos por los usuarios.
 *
 * No se fía del {@code Content-Type} que envía el navegador: el tipo real
 * se deduce de los primeros bytes del archivo ("magic numbers"). Así se
 * evita que alguien suba, por ejemplo, un HTML o un SVG con JavaScript
 * declarándolo como imagen y que luego se sirva desde nuestro dominio.
 * El tipo detectado es el que se guarda en {@code DBFile.fileType}.
 */
public final class UploadValidator {

    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String WEBP = "image/webp";
    public static final String PDF = "application/pdf";

    /** Fotografías de obras. */
    public static final Set<String> IMAGES = Set.of(JPEG, PNG, WEBP);
    /** Documentos: verificación, adjuntos de obras y del chat. */
    public static final Set<String> IMAGES_AND_PDF = Set.of(JPEG, PNG, WEBP, PDF);

    private UploadValidator() {
    }

    /**
     * Comprueba que el archivo no esté vacío y que su contenido real sea
     * uno de los tipos permitidos. Devuelve el tipo MIME detectado.
     */
    public static String requireType(MultipartFile file, Set<String> allowed) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio.");
        }
        String detected = detect(file);
        if (detected == null || !allowed.contains(detected)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Formatos admitidos: "
                    + describe(allowed) + ".");
        }
        return detected;
    }

    /**
     * Limpia el nombre original: quita rutas, caracteres de control y
     * comillas (se usa en la cabecera Content-Disposition) y lo recorta.
     */
    public static String safeFileName(String original, String fallback) {
        if (original == null || original.isBlank()) return fallback;
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (name.isEmpty()) return fallback;
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private static String detect(MultipartFile file) throws IOException {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        }
        byte[] h = Arrays.copyOf(head, read);

        if (startsWith(h, 0xFF, 0xD8, 0xFF)) return JPEG;
        if (startsWith(h, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return PNG;
        if (startsWith(h, 'R', 'I', 'F', 'F') && h.length >= 12
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return WEBP;
        if (startsWith(h, '%', 'P', 'D', 'F', '-')) return PDF;
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) return false;
        }
        return true;
    }

    private static String describe(Set<String> allowed) {
        return allowed.contains(PDF) ? "JPEG, PNG, WEBP o PDF" : "JPEG, PNG o WEBP";
    }
}
