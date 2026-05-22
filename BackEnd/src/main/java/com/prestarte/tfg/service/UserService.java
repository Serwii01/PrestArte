package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.EmailAlreadyExistsException;
import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.RegistrationRequest;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.DBFileRepository;
import com.prestarte.tfg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Servicio que gestiona el ciclo de vida de las cuentas de usuario.
 *
 * Centraliza el registro (con verificación de unicidad y de los
 * identificadores fiscales españoles), la aprobación o rechazo por
 * parte del administrador, la consulta de usuarios y la eliminación
 * controlada. Las contraseñas se cifran con BCrypt y el documento KYB
 * se persiste junto con la cuenta.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DBFileRepository dbFileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario en estado pendiente de aprobación.
     *
     * Comprueba la unicidad de email, teléfono y DNI/NIE/CIF, valida
     * que el identificador fiscal sea correcto, guarda el documento
     * de verificación adjunto y persiste el usuario con la subclase
     * adecuada en función del rol elegido. Tras crear la cuenta envía
     * un correo de bienvenida indicando que está en revisión.
     */
    @Transactional
    public User registerUser(RegistrationRequest request, MultipartFile file) throws IOException {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException(
                    "Ya existe una cuenta registrada con este teléfono.");
        }
        String taxId = request.getTaxId().toUpperCase().trim();
        if (!isValidSpanishTaxId(taxId)) {
            throw new IllegalArgumentException(
                    "El DNI / NIE / CIF no es válido (letra o dígito de control incorrectos).");
        }
        if (userRepository.existsByTaxId(taxId)) {
            throw new IllegalArgumentException(
                    "Ya existe una cuenta registrada con este DNI / NIE / CIF.");
        }
        request.setTaxId(taxId);

        validateVerificationFile(file);

        DBFile verificationDoc = DBFile.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .data(file.getBytes())
                .fileSize(file.getSize())
                .build();
        dbFileRepository.save(verificationDoc);

        User user = buildUserBySubclass(request);
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setTaxId(request.getTaxId());
        user.setVerificationFile(verificationDoc);
        user.setStatus(UserStatus.PENDING);
        user.setEnabled(false);

        User savedUser = userRepository.save(user);
        sendWelcomeEmail(savedUser);
        return savedUser;
    }

    /**
     * Aprueba la cuenta indicada, la marca como habilitada y envía al
     * usuario un correo informándole de la activación.
     */
    @Transactional
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", userId));

        user.setStatus(UserStatus.APPROVED);
        user.setEnabled(true);
        userRepository.save(user);
        sendActivationEmail(user);
    }

    /** Marca la cuenta como rechazada e impide que pueda iniciar sesión. */
    @Transactional
    public void rejectUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", userId));

        user.setStatus(UserStatus.REJECTED);
        user.setEnabled(false);
        userRepository.save(user);
    }

    /** Devuelve la lista de usuarios pendientes de revisión. */
    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    /** Devuelve todos los usuarios, opcionalmente filtrados por rol. */
    public List<User> getAllUsers(Role role) {
        return role == null ? userRepository.findAll() : userRepository.findByRole(role);
    }

    /**
     * Elimina un usuario de la plataforma.
     *
     * Si el usuario tiene contenido asociado (obras, préstamos o
     * envíos), la base de datos rechaza el borrado por integridad
     * referencial. En ese caso se traduce la excepción a un mensaje
     * claro indicando que primero hay que cancelar su actividad.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", userId));
        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "No se puede eliminar el usuario porque tiene contenido asociado " +
                    "(obras, préstamos o envíos). Rechaza o cancela su actividad antes de eliminarlo.");
        }
    }

    // ===== Helpers privados =====

    /** Instancia la subclase de {@link User} correspondiente al rol indicado. */
    private User buildUserBySubclass(RegistrationRequest request) {
        String roleStr = request.getRole().toUpperCase();
        switch (roleStr) {
            case "FOUNDATION":
            case "MUSEUM":
                Foundation foundation = new Foundation();
                foundation.setInstitutionName(request.getName());
                foundation.setRole(Role.FOUNDATION);
                return foundation;

            case "TRANSPORT":
                TransportCompany transport = new TransportCompany();
                transport.setCompanyName(request.getName());
                transport.setRole(Role.TRANSPORT);
                return transport;

            case "COLLECTOR":
            default:
                Collector collector = new Collector();
                collector.setRole(Role.COLLECTOR);
                return collector;
        }
    }

    private static final String DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    /**
     * Comprueba el formato y el dígito o letra de control de un
     * identificador fiscal español (DNI, NIE o CIF). Reproduce la misma
     * lógica que la validación que ejecuta el cliente, de modo que el
     * comportamiento sea consistente en ambos extremos.
     */
    private boolean isValidSpanishTaxId(String value) {
        if (value == null) return false;
        if (value.matches("\\d{8}[A-Z]")) {
            int num = Integer.parseInt(value.substring(0, 8));
            return DNI_LETTERS.charAt(num % 23) == value.charAt(8);
        }
        if (value.matches("[XYZ]\\d{7}[A-Z]")) {
            char p = value.charAt(0);
            int prefix = p == 'X' ? 0 : p == 'Y' ? 1 : 2;
            int num = Integer.parseInt(prefix + value.substring(1, 8));
            return DNI_LETTERS.charAt(num % 23) == value.charAt(8);
        }
        if (value.matches("[ABCDEFGHJNPQRSUVW]\\d{7}[0-9A-J]")) {
            char letter = value.charAt(0);
            String digits = value.substring(1, 8);
            char control = value.charAt(8);
            int even = 0, odd = 0;
            for (int i = 0; i < digits.length(); i++) {
                int d = digits.charAt(i) - '0';
                if ((i + 1) % 2 == 0) {
                    even += d;
                } else {
                    int dbl = d * 2;
                    odd += dbl > 9 ? dbl - 9 : dbl;
                }
            }
            int unit = (even + odd) % 10;
            int controlDigit = unit == 0 ? 0 : 10 - unit;
            char controlLetter = "JABCDEFGHI".charAt(controlDigit);
            if ("KPQSNRW".indexOf(letter) >= 0) return control == controlLetter;
            if ("ABEH".indexOf(letter) >= 0) return control == (char) ('0' + controlDigit);
            return control == (char) ('0' + controlDigit) || control == controlLetter;
        }
        return false;
    }

    /** Verifica que el documento de verificación venga presente y en un formato admitido. */
    private void validateVerificationFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de verificación es obligatorio");
        }
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("application/pdf")
                        || contentType.equals("image/jpeg")
                        || contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo PDF, JPEG o PNG.");
        }
    }

    /** Envía el correo informando al usuario de que su cuenta queda en revisión. */
    private void sendWelcomeEmail(User user) {
        String subject = "Bienvenid@ a Prestarte - Registro recibido";
        String body = "Hola " + user.getName() + ",\n\n" +
                "Gracias por registrarte. Tu perfil profesional está siendo revisado " +
                "por nuestro equipo de administración.\n\n" +
                "Te avisaremos por email cuando tu cuenta sea activada.\n\n" +
                "Un saludo,\nEl equipo de Prestarte.";
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    /** Envía el correo que confirma al usuario que su cuenta ha sido activada. */
    private void sendActivationEmail(User user) {
        String subject = "Tu cuenta de Prestarte ha sido activada";
        String body = "Enhorabuena " + user.getName() + ",\n\n" +
                "Tu cuenta ha sido verificada y activada. Ya puedes iniciar sesión " +
                "y participar en la red de préstamos de obras de arte.\n\n" +
                "¡Bienvenid@ a bordo!\nEl equipo de Prestarte.";
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }
}
